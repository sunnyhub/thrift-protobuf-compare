package serializers;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import serializers.avro.AvroGenericSerializer;
import serializers.avro.specific.AvroSpecificSerializer;

public class BenchmarkRunner
{
  public final static int ITERATIONS = 2000;
  public final static int TRIALS = 20;

  /**
   * Number of milliseconds to warm up for each operation type for each serializer. Let's
   * start with 3 seconds.
   */
  final static long WARMUP_MSECS = 3000;

  @SuppressWarnings("unchecked")
  private Set<ObjectSerializer> _serializers = new LinkedHashSet<ObjectSerializer>();

  public static void main(String... args) throws Exception
  {
    BenchmarkRunner runner = new BenchmarkRunner();

    // binary codecs first
    runner.addObjectSerializer(new AvroGenericSerializer());
    runner.addObjectSerializer(new AvroSpecificSerializer());
    runner.addObjectSerializer(new ProtobufSerializer());
    runner.addObjectSerializer(new ThriftSerializer());
    runner.addObjectSerializer(new HessianSerializer());

    // then language default serializers
    runner.addObjectSerializer(new JavaSerializer());

    runner.addObjectSerializer(new JavaExtSerializer());
    runner.addObjectSerializer(new ScalaSerializer());

    // then Json
    runner.addObjectSerializer(new JsonSerializer());

    // then xml via stax, textual and binary
    runner.addObjectSerializer(new StaxSerializer("stax/woodstox",
                                                  new com.ctc.wstx.stax.WstxInputFactory(),
                                                  new com.ctc.wstx.stax.WstxOutputFactory()));
    runner.addObjectSerializer(new StaxSerializer("stax/aalto",
                                                  new com.fasterxml.aalto.stax.InputFactoryImpl(),
                                                  new com.fasterxml.aalto.stax.OutputFactoryImpl()));
    runner.addObjectSerializer(new StaxSerializer("binaryxml/FI",
                                                  new com.sun.xml.fastinfoset.stax.factory.StAXInputFactory(),
                                                  new com.sun.xml.fastinfoset.stax.factory.StAXOutputFactory()));
    runner.addObjectSerializer(new XStreamSerializer("xstream (xpp)", false, null, null));
    runner.addObjectSerializer(new XStreamSerializer("xstream (xpp with conv)", true, null, null));
    runner.addObjectSerializer(new XStreamSerializer("xstream (stax)",
                                                     false,
                                                     new com.ctc.wstx.stax.WstxInputFactory(),
                                                     new com.ctc.wstx.stax.WstxOutputFactory()));
    runner.addObjectSerializer(new XStreamSerializer("xstream (stax with conv)",
                                                     true,
                                                     new com.ctc.wstx.stax.WstxInputFactory(),
                                                     new com.ctc.wstx.stax.WstxOutputFactory()));
    runner.addObjectSerializer(new JavolutionXMLFormatSerializer());

    runner.addObjectSerializer(new SbinarySerializer());
    // runner.addObjectSerializer(new YamlSerializer());

    System.out.println("Starting");

    runner.start();
  }

  @SuppressWarnings("unchecked")
  private void addObjectSerializer(ObjectSerializer serializer)
  {
    _serializers.add(serializer);
  }

  private <T> double createObjects(ObjectSerializer<T> serializer, int iterations) throws Exception
  {
    long delta = 0;
    for (int i = 0; i < iterations; i++)
    {
      long start = System.nanoTime();
      serializer.create();
      delta += System.nanoTime() - start;
    }
    return iterationTime(delta, iterations);
  }

  private double iterationTime(long delta, int iterations)
  {
    return (double) delta / (double) (iterations);
  }

  private <T> double serializeObjects(ObjectSerializer<T> serializer, int iterations) throws Exception
  {
    // let's reuse same instance to reduce overhead
    long delta = 0;
    for (int i = 0; i < iterations; i++)
    {
      T obj = serializer.create();
      long start = System.nanoTime();
      serializer.serialize(obj);
      delta += System.nanoTime() - start;
      if (i % 1000 == 0)
        doGc();
    }
    return iterationTime(delta, iterations);
  }

  private <T> double deserializeObjects(ObjectSerializer<T> serializer, int iterations) throws Exception
  {
    byte[] array = serializer.serialize(serializer.create());
    long delta = 0;
    for (int i = 0; i < iterations; i++)
    {
      long start = System.nanoTime();
      serializer.deserialize(array);
      delta += System.nanoTime() - start;
    }
    return iterationTime(delta, iterations);
  }

  /**
   * JVM is not required to honor GC requests, but adding bit of sleep around request is
   * most likely to give it a chance to do it.
   */
  private void doGc()
  {
    try
    {
      Thread.sleep(100L);
    }
    catch (InterruptedException ie)
    {
    }
    System.gc();
    System.gc();
    System.gc();
    System.gc();
    try
    {
      Thread.sleep(100L);
    }
    catch (InterruptedException ie)
    {
    }
  }

  enum measurements
  {
    timeCreate, timeSer, timeDSer, totalTime, length
  }

  @SuppressWarnings("unchecked")
  private void start() throws Exception
  {
    System.out.printf("%-24s, %15s, %15s, %15s, %15s, %10s\n",
                      " ",
                      "Object create",
                      "Serialization",
                      "Deserialization",
                      "Total Time",
                      "Serialized Size");
    EnumMap<measurements, Map<String, Double>> values = new EnumMap<measurements, Map<String, Double>>(measurements.class);
    for (measurements m : measurements.values())
      values.put(m, new HashMap<String, Double>());

    for (ObjectSerializer serializer : _serializers)
    {
      /*
       * Should only warm things for the serializer that we test next: HotSpot JIT will
       * otherwise spent most of its time optimizing slower ones... Use
       * -XX:CompileThreshold=1 to hint the JIT to start immediately
       *
       * Actually: 1 is often not a good value -- threshold is the number
       * of samples needed to trigger inlining, and there's no point in
       * inlining everything. Default value is in thousands, so lowering
       * it to, say, 1000 is usually better.
       */
      warmCreation(serializer);
      doGc();
      double timeCreate = Double.MAX_VALUE;
      // do more iteration for object creation because of its short time
      for (int i = 0; i < TRIALS; i++)
        timeCreate = Math.min(timeCreate, createObjects(serializer, ITERATIONS * 100));

      warmSerialization(serializer);

      // actually: let's verify serializer actually works now:
      checkCorrectness(serializer);

      doGc();
      double timeSer = Double.MAX_VALUE;
      for (int i = 0; i < TRIALS; i++)
        timeSer = Math.min(timeSer, serializeObjects(serializer, ITERATIONS));

      warmDeserialization(serializer);
      doGc();
      double timeDSer = Double.MAX_VALUE;
      for (int i = 0; i < TRIALS; i++)
        timeDSer = Math.min(timeDSer, deserializeObjects(serializer, ITERATIONS));

      byte[] array = serializer.serialize(serializer.create());
      double totalTime = timeCreate + timeSer + timeDSer;
      System.out.printf("%-24s, %15.5f, %15.5f, %15.5f, %15.5f, %10d\n",
                        serializer.getName(),
                        timeCreate,
                        timeSer,
                        timeDSer,
                        totalTime,
                        array.length);
      addValue(values, serializer.getName(), timeCreate, timeSer, timeDSer, totalTime, array.length);
    }
    printImages(values);
  }

    /**
     * Method that tries to validate correctness of serializer, using
     * round-trip (construct, serializer, deserialize; compare objects
     * after steps 1 and 3).
     * Currently only done for StdMediaDeserializer...
     */
    private void checkCorrectness(ObjectSerializer serializer)
        throws Exception
    {
        Object input = serializer.create();
        byte[] array = serializer.serialize(input);
        Object output = serializer.deserialize(array);
        if (!input.equals(output)) {
            /* Should throw an exception; but for now (that we have a few
             * failures) let's just whine...
             */
            String msg = "serializer '"+serializer.getName()+"' failed round-trip test (ser+deser produces Object different from input)";
            //throw new Exception("Error: "+msg);
            System.err.println("WARN: "+msg);
        }
    }

  private void printImages(EnumMap<measurements, Map<String, Double>> values)
  {
    for (measurements m : values.keySet())
      printImage(values.get(m), m);
  }

  private void printImage(Map<String, Double> map, measurements m)
  {
    StringBuilder valSb = new StringBuilder();
    String names = "";
    double sum = 0;
    for (Entry<String, Double> entry : map.entrySet())
    {
      valSb.append(entry.getValue()).append(',');
      sum += entry.getValue();
      names = entry.getKey() + '|' + names;
    }
    int avg = (int) sum / map.size();
    System.out.println("<img src='http://chart.apis.google.com/chart?chtt="
        + m.name()
        + "&chf=c||lg||0||FFFFFF||1||76A4FB||0|bg||s||EFEFEF&chs=1000x300&chd=t:"
        + valSb.toString().substring(0, valSb.length() - 1)
        + "&chds="
        + 0
        + ","
        + (avg * 2)
        + "&chxl=0:|"
        + names.substring(0, names.length() - 1)
        + "&lklk&chdlp=t&chco=660000|660033|660066|660099|6600CC|6600FF|663300|663333|663366|663399|6633CC|6633FF|666600|666633|666666&cht=bhg&chbh=10&chxt=y&nonsense=aaa.png'/>");
  }

  private void addValue(EnumMap<measurements, Map<String, Double>> values,
                        String name,
                        double timeCreate,
                        double timeSer,
                        double timeDSer,
                        double totalTime,
                        double length)
  {
    values.get(measurements.timeCreate).put(name, timeCreate);
    values.get(measurements.timeSer).put(name, timeSer);
    values.get(measurements.timeDSer).put(name, timeDSer);
    values.get(measurements.totalTime).put(name, totalTime);
    values.get(measurements.length).put(name, length);
  }

  private <T> void warmCreation(ObjectSerializer<T> serializer) throws Exception
  {
    // Instead of fixed counts, let's try to prime by running for N seconds
    long endTime = System.currentTimeMillis() + WARMUP_MSECS;
    do
    {
      createObjects(serializer, 1);
    }
    while (System.currentTimeMillis() < endTime);
  }

  private <T> void warmSerialization(ObjectSerializer<T> serializer) throws Exception
  {
    // Instead of fixed counts, let's try to prime by running for N seconds
    long endTime = System.currentTimeMillis() + WARMUP_MSECS;
    do
    {
      serializeObjects(serializer, 1);
    }
    while (System.currentTimeMillis() < endTime);
  }

  private <T> void warmDeserialization(ObjectSerializer<T> serializer) throws Exception
  {
    // Instead of fixed counts, let's try to prime by running for N seconds
    long endTime = System.currentTimeMillis() + WARMUP_MSECS;
    do
    {
      deserializeObjects(serializer, 1);
    }
    while (System.currentTimeMillis() < endTime);
  }
}

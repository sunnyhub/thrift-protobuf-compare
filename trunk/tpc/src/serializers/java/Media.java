package serializers.java;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.twolattes.json.Entity;
import com.twolattes.json.Value;
import org.codehaus.jackson.annotate.JsonProperty;

@Entity
public class Media implements Serializable
{
  private static final long serialVersionUID = 1L;

  public enum Player
  {
    JAVA, FLASH
  }

   // Note: MUST use names from StdMediaSerializer (FIELD_NAME_xxx)

  @Value(name = "pl", ordinal = true)
  private Player _player;

  @Value(name = "ul")
  private String _uri;

  private @Value(name = "tl") String _title;
  private @Value(name = "wd") int _width;
  private @Value(name = "hg") int _height;
  private @Value(name = "fr") String _format;
  private @Value(name = "dr") long _duration;
  private @Value(name = "sz") long _size;
  private @Value(name = "br") int _bitrate;
  private @Value(name = "pr") List<String> _persons;
  private @Value(name = "c") String _copyright;

  public Media(){}
  
  public Media (String copyright,
                String format,
                Player player,
                String title,
                String uri,
                long duration,
                long size,
                int height,
                int width,
                int bitrate)
  {
    _copyright = copyright;
    _duration = duration;
    _format = format;
    _height = height;
    _player = player;
    _size = size;
    _title = title;
    _uri = uri;
    _width = width;
    _bitrate = bitrate;
  }

  @JsonProperty("p1")
  public Player getPlayer ()
  {
    return _player;
  }

  @JsonProperty("p1")
  public void setPlayer (Player player)
  {
    _player = player;
  }

  @JsonProperty("u1")
  public String getUri ()
  {
    return _uri;
  }

  @JsonProperty("u1")
  public void setUri (String uri)
  {
    _uri = uri;
  }

  @JsonProperty("tl")
  public String getTitle ()
  {
    return _title;
  }

  @JsonProperty("tl")
  public void setTitle (String title)
  {
    _title = title;
  }

  @JsonProperty("wd")
  public int getWidth ()
  {
    return _width;
  }

  @JsonProperty("wd")
  public void setWidth (int width)
  {
    _width = width;
  }

  @JsonProperty("hg")
  public int getHeight ()
  {
    return _height;
  }

  @JsonProperty("hg")
  public void setHeight (int height)
  {
    _height = height;
  }

  @JsonProperty("fr")
  public String getFormat ()
  {
    return _format;
  }

  @JsonProperty("fr")
  public void setFormat (String format)
  {
    _format = format;
  }

  @JsonProperty("dr")
  public long getDuration ()
  {
    return _duration;
  }

  @JsonProperty("dr")
  public void setDuration (long duration)
  {
    _duration = duration;
  }

  @JsonProperty("sz")
  public long getSize ()
  {
    return _size;
  }

  @JsonProperty("sz")
  public void setSize (long size)
  {
    _size = size;
  }

  @JsonProperty("br")
  public int getBitrate ()
  {
    return _bitrate;
  }

  @JsonProperty("br")
  public void setBitrate (int bitrate)
  {
    this._bitrate = bitrate;
  }

  @JsonProperty("pr")
  public List<String> getPersons ()
  {
      return _persons;
  }

  @JsonProperty("pr")
  public void setPersons(List<String> p)
  {
      _persons = p;
  }

  public void addToPerson (String person)
  {
    if (null == _persons)
    {
      _persons = new ArrayList<String>();
    }
    _persons.add(person);
  }

  @JsonProperty("c")
  public String getCopyright ()
  {
    return _copyright;
  }

  @JsonProperty("c")
  public void setCopyright (String copyright)
  {
    _copyright = copyright;
  }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (!(o instanceof Media)) return false;

        Media other = (Media) o;

        if (_width != other._width) return false;
        if (_height != other._height) return false;
        if (_duration != other._duration) return false;
        if (_size != other._size) return false;
        if (_bitrate != other._bitrate) return false;
        if (!_player.equals(other._player)) return false;
        if (!_uri.equals(other._uri)) return false;
        if (!_title.equals(other._title)) return false;
        if (!_format.equals(other._format)) return false;
        if (!_persons.equals(other._persons)) return false;

        if (_copyright == null) {
            if (other._copyright != null) return false;
        } else {
            if (!_copyright.equals(other._copyright)) return false;
        }
        return true;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("[Media ");
        sb.append("width=").append(_width);
        sb.append(", height=").append(_height);
        sb.append(", duration=").append(_duration);
        sb.append(", size=").append(_size);
        sb.append(", bitrate=").append(_bitrate);
        sb.append(", player=").append(_player);
        sb.append(", uri=").append(_uri);
        sb.append(", title=").append(_title);
        sb.append(", format=").append(_format);
        sb.append(", persons=").append(_persons);
        sb.append(", copyright=").append(_copyright);
        sb.append("]");
        return sb.toString();
    }
}
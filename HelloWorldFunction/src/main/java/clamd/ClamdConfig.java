package clamd;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClamdConfig {
    private String hostname;
    private int port;
    private int timeout;
    private String maxfilesize;
    private String maxrequestsize;
}
package sit.int221.mytasksservice.dtos.response.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CollabResponseDTO {
    private Integer collabId;
    private String oid;
    private String name;
    private String email;
    private String accessRight;
    private String boardsId;
    private String status;
    private Timestamp added_on;
    private Timestamp updated_on;
    private String token;
}

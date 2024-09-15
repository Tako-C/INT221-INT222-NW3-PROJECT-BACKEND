package sit.int221.mytasksservice.services;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.mytasksservice.dtos.response.request.BoardsAddRequestDTO;
import sit.int221.mytasksservice.dtos.response.response.BoardsResponseDTO;
import sit.int221.mytasksservice.dtos.response.response.ItemNotFoundException;
import sit.int221.mytasksservice.models.primary.Boards;
import sit.int221.mytasksservice.models.secondary.Users;
import sit.int221.mytasksservice.repositories.primary.BoardsRepository;
import sit.int221.mytasksservice.repositories.secondary.UsersRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BoardsService {
    @Autowired
    private BoardsRepository boardsRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private UsersRepository usersRepository;

    public Boards getBoards(String boards){
        return boardsRepository.findById(boards).orElseThrow(ItemNotFoundException::new);
    }

    public List<BoardsResponseDTO> getAllBoards(){
        List<Boards> boards = boardsRepository.findAll();
        return boards.stream().map(board ->
                modelMapper.map(board, BoardsResponseDTO.class)
        ).collect(Collectors.toList());
    }

    public Boards createBoards(BoardsAddRequestDTO boardsAddRequestDTO){
        Boards boards = modelMapper.map(boardsAddRequestDTO, Boards.class);
        if (boards.getBoard_name() == null || boards.getBoard_name().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (boards.getBoard_name().length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return boardsRepository.save(boards);
    }

    public Users findByOid(String oid) {
        Users user = usersRepository.findByOid(oid);
        if (user == null) {
            throw new ItemNotFoundException();
        }
        return user;
    }
}

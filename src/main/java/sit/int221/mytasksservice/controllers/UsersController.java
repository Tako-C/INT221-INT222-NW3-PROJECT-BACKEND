package sit.int221.mytasksservice.controllers;

import jakarta.validation.Valid;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sit.int221.mytasksservice.config.JwtTokenUtil;
import sit.int221.mytasksservice.dtos.response.request.JwtRequestDTO;
import sit.int221.mytasksservice.dtos.response.response.RefreshTokenDTO;
import sit.int221.mytasksservice.dtos.response.response.UsersDTO;
import sit.int221.mytasksservice.dtos.response.response.jwtResponseDTO;
import sit.int221.mytasksservice.models.secondary.Users;
import sit.int221.mytasksservice.services.JwtUserDetailsService;
import sit.int221.mytasksservice.services.UsersService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = {"http://localhost:5173","http://ip23nw3.sit.kmutt.ac.th:3333","http://intproj23.sit.kmutt.ac.th"})

public class UsersController {

    @Autowired
    private UsersService usersService;

    @Autowired
    private JwtUserDetailsService jwtUserDetailsService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private ModelMapper modelMapper;

    @GetMapping("/users")
    public List<UsersDTO> getAllUsers() {
        List<Users> users = usersService.getAllUsers();
        return users.stream()
                .map(us -> modelMapper.map(us, UsersDTO.class))
                .collect(Collectors.toList());

    }

    @PostMapping("/login")
    public ResponseEntity<jwtResponseDTO> login(@Valid @RequestBody JwtRequestDTO jwtRequestDTO) {
        Users user = usersService.login(jwtRequestDTO.getUserName(), jwtRequestDTO.getPassword());

        UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(jwtRequestDTO.getUserName());
        String accessToken = jwtTokenUtil.generateToken(userDetails);
        String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

        usersService.saveUserToPrimary(user);

        jwtResponseDTO responseTokenDTO = new jwtResponseDTO();
        responseTokenDTO.setAccess_token(accessToken);
        responseTokenDTO.setRefresh_token(refreshToken);

        return ResponseEntity.ok(responseTokenDTO);
    }

    @PostMapping("/token")
    public ResponseEntity<RefreshTokenDTO> refreshAccessToken(@RequestHeader("Authorization") String refreshToken) {
        if (jwtTokenUtil.validateRefreshToken(refreshToken.substring(7))) {
            String oid = jwtTokenUtil.getOid(refreshToken.substring(7));
            UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(oid);

            String newAccessToken = jwtTokenUtil.generateToken(userDetails);

            RefreshTokenDTO responseTokenDTO = new RefreshTokenDTO();
            responseTokenDTO.setAccess_token(newAccessToken);

            return ResponseEntity.ok(responseTokenDTO);
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token");
        }
    }
}

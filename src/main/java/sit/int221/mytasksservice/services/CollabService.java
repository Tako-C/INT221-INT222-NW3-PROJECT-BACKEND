package sit.int221.mytasksservice.services;

import jakarta.mail.MessagingException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sit.int221.mytasksservice.dtos.response.request.InvitationRequestDTO;
import sit.int221.mytasksservice.dtos.response.response.*;
import sit.int221.mytasksservice.models.primary.*;
import sit.int221.mytasksservice.models.secondary.Users;
import sit.int221.mytasksservice.repositories.primary.BoardsRepository;
import sit.int221.mytasksservice.repositories.primary.CollabBoardRepository;
import sit.int221.mytasksservice.repositories.secondary.UsersRepository;

import java.io.UnsupportedEncodingException;
import java.util.Optional;
import java.util.UUID;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CollabService {

    @Autowired
    private CollabBoardRepository collabBoardRepository;

    @Autowired
    private BoardsRepository boardsRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ModelMapper modelMapper;

    @Transactional
    public CollabBoard sendInvitation(String boardId, InvitationRequestDTO invitationRequestDTO, String inviterUsername) {
        Boards board = boardsRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board not found"));

        Users inviter = usersRepository.findByUsername(inviterUsername)
                .orElseThrow(() -> new ItemNotFoundException("Inviter not found"));

        Users invitee = usersRepository.findByEmail(invitationRequestDTO.getInviteeEmail())
                .orElseThrow(() -> new ItemNotFoundException("Invitee not found"));

        if (!board.getOid().equals(inviter.getOid()) &&
                !collabBoardRepository.existsByOidAndBoardsIdAndStatusInvite(inviter.getOid(), boardId, InviteStatus.ACCEPTED)) {
            throw new ForbiddenException("Only the board owner or accepted collaborators can send invitations.");
        }

        boolean existsPending = collabBoardRepository.existsByOidAndBoardsIdAndStatusInvite(
                invitee.getOid(),
                boardId,
                InviteStatus.PENDING
        );

        boolean isAlreadyCollaborator = collabBoardRepository.existsByOidAndBoardsIdAndStatusInvite(
                invitee.getOid(),
                boardId,
                InviteStatus.ACCEPTED
        );

        if (existsPending || isAlreadyCollaborator) {
            throw new DuplicateItemException("The user is already a collaborator or pending collaborator of this board");
        }

        if (invitee.getOid().equals(board.getOid())) {
            throw new DuplicateItemException("The board owner cannot be added as a collaborator.");
        }

        CollabBoard invitation = new CollabBoard();
        invitation.setOid(invitee.getOid());
        invitation.setName(invitee.getName());
        invitation.setEmail(invitee.getEmail());
        invitation.setAccessRight(invitationRequestDTO.getAccessRight());
        invitation.setBoardsId(boardId);
        invitation.setStatusInvite(InviteStatus.PENDING);
        invitation.setToken(UUID.randomUUID().toString());

        CollabBoard savedInvitation = collabBoardRepository.save(invitation);

        // emailsender here
        try {
            emailService.sendInvitationEmailWithReplyTo(inviter, invitee, board, invitation.getAccessRight(), invitation.getToken());
        } catch (MessagingException e) {
            throw new EmailSendingException(String.format(
                    "We could not send e-mail to %s, he/she can accept the invitation at %s/board/%s/collab/invitations/accept?token=%s or decline at %s/board/%s/collab/invitations/decline?token=%s",
                    invitee.getName(),
                    getFrontendUrl(),
                    board.getBoardId(),
                    invitation.getToken(),
                    getFrontendUrl(),
                    board.getBoardId(),
                    invitation.getToken()
            ));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return savedInvitation;
    }

    @Transactional
    public CollabBoard acceptInvitation(String token, String inviteeUsername) {
        Optional<CollabBoard> optionalInvitation = collabBoardRepository.findByToken(token);

        if (!optionalInvitation.isPresent()) {
            throw new ItemNotFoundException("Invitation not found");
        }

        CollabBoard invitation = optionalInvitation.get();

        Users invitee = usersRepository.findByUsername(inviteeUsername)
                .orElseThrow(() -> new ItemNotFoundException("Invitee not found"));

//        if (!invitation.getOid().equals(invitee.getOid())) {
//            throw new ForbiddenException("You are not authorized to accept this invitation");
//        }

        if (!InviteStatus.PENDING.equals(invitation.getStatusInvite())) {
            throw new ForbiddenException("Invitation is not active");
        }

        invitation.setStatusInvite(InviteStatus.ACCEPTED);
        CollabBoard savedInvitation = collabBoardRepository.save(invitation);
        return savedInvitation;
    }

    @Transactional
    public CollabBoard declineInvitation(String token, String inviteeUsername) {
        Optional<CollabBoard> optionalInvitation = collabBoardRepository.findByToken(token);

        if (!optionalInvitation.isPresent()) {
            throw new ItemNotFoundException("Invitation not found");
        }

        CollabBoard invitation = optionalInvitation.get();

        Users invitee = usersRepository.findByUsername(inviteeUsername)
                .orElseThrow(() -> new ItemNotFoundException("Invitee not found"));

//        if (!invitation.getOid().equals(invitee.getOid())) {
//            throw new ForbiddenException("You are not authorized to decline this invitation");
//        }

        if (!InviteStatus.PENDING.equals(invitation.getStatusInvite())) {
            throw new ForbiddenException("Invitation is not active");
        }

        CollabBoard savedInvitation = collabBoardRepository.save(invitation);
        return savedInvitation;
    }

    // getAll (pending and accepted)
    @Transactional(readOnly = true)
    public CollabListResponseDTO getAllCollabs(String boardId) {
        // ดึงข้อมูล Collaborators
        List<CollabBoard> collaborators = collabBoardRepository.findByBoardsId(boardId);

        // ดึงข้อมูล Board
        Boards board = boardsRepository.findById(boardId)
                .orElseThrow(() -> new ItemNotFoundException("Board not found"));

        // ดึงข้อมูล Owner โดยใช้ users_oid จาก Board
        String ownerOid = board.getOid();
        Users owner = usersRepository.findByOid(ownerOid);

        // แปลงข้อมูล Owner เป็น OwnerDTO
        Owner ownerDTO = modelMapper.map(owner, Owner.class);

        // แปลง Collaborators เป็น CollabResponseDTO
        List<CollabResponseDTO> collabDTOs = collaborators.stream()
                .map(collab -> modelMapper.map(collab, CollabResponseDTO.class))
                .collect(Collectors.toList());

        // สร้าง CollabListResponseDTO
        CollabListResponseDTO response = new CollabListResponseDTO();
        response.setBoardName(board.getBoard_name());
        response.setOwner(ownerDTO);
        response.setCollaborators(collabDTOs);

        return response;
    }

    public CollabResponseDTO getCollabByOid(String boardId, String collabOid) {
        CollabBoard collab = collabBoardRepository.findCollabByOidAndBoardsId(collabOid, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found"));
        return modelMapper.map(collab, CollabResponseDTO.class);
    }

    @Transactional
    public CollabResponseDTO removeCollabFromBoard(String boardId, String collabOid) {
        CollabBoard collab = collabBoardRepository.findCollabByOidAndBoardsId(collabOid, boardId)
                .orElseThrow(() -> new ItemNotFoundException("Collaborator not found"));
        collabBoardRepository.delete(collab);
        return modelMapper.map(collab, CollabResponseDTO.class);
    }

    // get frontend url from app.prop อย่าลืมเปลี่ยนตอน deploy
    private String getFrontendUrl() {
        return "http://localhost:5173";
    }
}

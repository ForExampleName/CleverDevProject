package com.example.oldsystem.api;

import com.example.oldsystem.dto.ClientDto;
import com.example.oldsystem.repository.old.ClientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/clients")
public class ClientController {
    private final ClientRepository clientRepository;

    public ClientController(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @PostMapping // why not get ? because post is in the task description
    public ResponseEntity<List<ClientDto>> getClients() {
        //without service usage for simplicity
        List<ClientDto> responseDto = clientRepository.findAll().stream()
                .map(client -> ClientDto.builder()
                        .guid(client.getId().toString())
                        .agency(client.getAgency().getName())
                        .firstName(client.getFirstName())
                        .lastName(client.getLastName())
                        .status(client.getStatus())
                        .dob(client.getBirthDate().toString())
                        .createdDateTime(client.getCreatedAt().toString())
                        .build()
                ).toList();

        // 404 not_found is absent because logic is like get all items (empty list as the result is possible)

        return ResponseEntity.ok(responseDto);
    }
}

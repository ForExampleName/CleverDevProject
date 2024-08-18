package com.example.oldsystem.repository.old;

import com.example.oldsystem.entity.old.Client;
import org.springframework.data.repository.ListCrudRepository;

import java.util.UUID;

public interface ClientRepository extends ListCrudRepository<Client, UUID> {
}

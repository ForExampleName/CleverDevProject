package com.example.testproject.service;

import com.example.testproject.dto.ClientDto;
import com.example.testproject.exception.ConnectionException;
import com.example.testproject.exception.DataFetchException;

import java.util.List;

public interface ClientService {
    List<ClientDto> fetchClientData() throws DataFetchException, ConnectionException;

    void filterInvalidClientData(List<ClientDto> clients);

    int createNewClients(List<ClientDto> clients);
}

package server.service;

import server.network.ClientHandler;
import shared.model.Message;

public interface IMessageService {
    void handleMessage(Message m, ClientHandler sender);
}
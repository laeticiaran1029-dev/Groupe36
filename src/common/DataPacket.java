package common;

import java.io.Serializable;
import java.util.List;

public class DataPacket implements Serializable {
    private static final long serialVersionUID = 1L;

    // Types de messages possibles
    public enum Type {
        LOGIN,              // Connexion au serveur
        REQUEST_CONTROL,    // Demander à contrôler
        ACCEPT_CONTROL,     // Accepter le contrôle
        SCREEN_DATA,        // Données d'image (vidéo)
        MOUSE_MOVE,         // Mouvement souris
        MOUSE_CLICK,        // Clic souris
        UPDATE_LIST         // Mise à jour de la liste des clients
    }

    public Type type;
    public String sender;
    public String target;
    
    // Données variables (Payload)
    public byte[] imageBytes; // Pour l'image compressée
    public int x, y;          // Pour la souris
    public int mouseButton;   // 1=Gauche, 3=Droit

    // Pour UPDATE_LIST
    public List<String> connectedUsers;

    public DataPacket(Type type, String sender) {
        this.type = type;
        this.sender = sender;
    }

    // Constructeur vide utile pour la désérialisation
    public DataPacket() { }
}
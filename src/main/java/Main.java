import server.Server;
import client.Client;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Введите режим запуска: server или client");
            return;
        }

        String launchMode = args[0];
        switch (launchMode.toLowerCase()) {
            case "server":
                Server.main(args);
                break;
            case "client":
                Client.main(args);
                break;
            default:
                System.out.println("Ошибка ввода. Используйте команду server или client");
        }
    }
}

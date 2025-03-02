package server;

import common.Topic;
import common.Vote;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.*;

public class ServerHandler extends SimpleChannelInboundHandler<String>{ // определяем, что обработчик принимает только строки
    private enum CurrentState{ // список возможных состояний
        MENU,
        WAITING_FOR_NAME,
        WAITING_FOR_DESC,
        WAITING_FOR_QUANTITY,
        WAITING_FOR_OPTIONS,
        WAITING_FOR_VOTE
    }

    private static class ClientContext{ // вложенный класс для хранения введенных данных
        private String username;
        CurrentState currentState = CurrentState.MENU;
        boolean isLogged = false;
        String currentTopic;
        String voteName;
        String voteDescription;
        int optionsQuantity;
        Map<String, Integer> voteOptions = new HashMap<>();
        List<String> currentOptions = new ArrayList<>();
    }

    private final Map<ChannelHandlerContext, ClientContext> clientContexts = new HashMap<>(); // храним существующие состояния клиентов

    private void handleCommand(ChannelHandlerContext ctx, String msg, ClientContext context){
        String[] messageParts = msg.split(" "); // для разделения строки на команды
        String command = messageParts[0]; // устанавливаем переменную для первой команды

        switch (command.toLowerCase()){
            case "login":
                if(context.isLogged){
                    ctx.writeAndFlush("Вы уже авторизованы\n");
                    return;
                }
                handleLogin(ctx, messageParts, context);
                break;
            case "create":
                if(context.isLogged) {
                    handleCreate(ctx, messageParts, context);
                }else{
                    ctx.writeAndFlush("Сперва необходимо авторизоваться с помощью команды login -u=username, где username - имя вашего пользователя\n");
                }
                break;
            case "view":
                if(context.isLogged) {
                    handleView(ctx, messageParts);
                }else{
                    ctx.writeAndFlush("Сперва необходимо авторизоваться с помощью команды login -u=username, где username - имя вашего пользователя\n");
                }
                break;
            case "vote":
                if(context.isLogged) {
                    handleVote(ctx, messageParts, context);
                }else{
                    ctx.writeAndFlush("Сперва необходимо авторизоваться с помощью команды login -u=username, где username - имя вашего пользователя\n");
                }
                break;
            case "delete":
                if(context.isLogged) {
                    handleDelete(ctx, messageParts, context);
                }else{
                    ctx.writeAndFlush("Сперва необходимо авторизоваться с помощью команды login -u=username, где username - имя вашего пользователя\n");
                }
                break;
            case "exit":
                if(context.isLogged) {
                    handleExit(ctx);
                }else{
                    ctx.writeAndFlush("Сперва необходимо авторизоваться с помощью команды login -u=username, где username - имя вашего пользователя\n");
                }
                break;
            default:
                ctx.writeAndFlush("Введена несуществующая команда\n");
        }
    }

    private void handleLogin(ChannelHandlerContext ctx, String[] messageParts, ClientContext context){
        if(messageParts.length > 1 && messageParts[1].split("=")[0].equals("-u")){
            if(messageParts[1].split("=").length == 2) {
                String username = messageParts[1].split("=")[1];
                context.isLogged = true;
                context.username = username;
                ctx.writeAndFlush("Вы вошли под пользователем " + username + "\n");
                // лог
            }else{
                ctx.writeAndFlush("Ошибка ввода имени пользователя username\n");
            }
        }else{
            ctx.writeAndFlush("Неправильно введена команда login -u=username\n");
        }
    }

    private void handleCreate(ChannelHandlerContext ctx, String[] messageParts, ClientContext context){
        if (messageParts.length > 1 && messageParts[1].equalsIgnoreCase("topic")) {
            String topicName = messageParts[2].split("=")[1]; // получаем название раздела голосования
            synchronized (Server.getTopics()) { // обеспечение потокобезопасности
                if (!Server.getTopics().containsKey(topicName)) {
                    Server.getTopics().put(topicName, new Topic(topicName, context.username)); // создаем новый раздел
                    ctx.writeAndFlush("Создан раздел голосования: " + topicName + "\n");
                    // добавить логгирование
                } else {
                    ctx.writeAndFlush("Раздел с таким именем уже существует");
                }
            }
        }else if(messageParts.length > 1 && messageParts[1].equalsIgnoreCase("vote")){
            if(messageParts.length > 2 && messageParts[2].split("=")[0].equals("-t")){
                String topicName = messageParts[2].split("=")[1];
                synchronized (Server.getTopics()) {
                    if (Server.getTopics().containsKey(topicName)) {
                        context.currentTopic = topicName;
                        context.currentState = CurrentState.WAITING_FOR_NAME;
                        ctx.writeAndFlush("Создание нового голосования в разделе " + topicName + "\n Введите название голосования:");
                    } else {
                        ctx.writeAndFlush("Раздела с таким именем не существует\n");
                    }
                }
            }else {
                ctx.writeAndFlush("Неправильно введена команда create vote -n=topic, где topic - название раздела голосования\n");
            }
        }else{
            ctx.writeAndFlush("Для создания темы укажите ключевое слово topic\nДля создания голосования укажите ключевое слово vote и параметр темы -t=topic, где topic - название раздела\n");
        }
    }

    private void handleVoteCreation(ChannelHandlerContext ctx, String msg, ClientContext context){
        switch(context.currentState){
            case WAITING_FOR_NAME:
                context.voteName = msg;
                context.currentState = CurrentState.WAITING_FOR_DESC;
                ctx.writeAndFlush("Введите описание к голосованию\n");
                break;
            case WAITING_FOR_DESC:
                context.voteDescription = msg;
                context.currentState = CurrentState.WAITING_FOR_QUANTITY;
                ctx.writeAndFlush("Введите количество возможных ответов\n");
                break;
            case WAITING_FOR_QUANTITY:
                try{
                    if(Integer.parseInt(msg) > 0){
                        context.optionsQuantity = Integer.parseInt(msg);
                        context.currentState = CurrentState.WAITING_FOR_OPTIONS;
                        ctx.writeAndFlush("Введите вариант ответа 1\n");
                    }else{
                        ctx.writeAndFlush("Должен быть хотя бы один вариант ответа\n");
                    }
                }catch (NumberFormatException e){
                    ctx.writeAndFlush("Ошибка ввода. Введите число возможных ответов\n");
                }
                break;
            case WAITING_FOR_OPTIONS:
                if(!context.voteOptions.containsKey(msg.toLowerCase())) { // делаем каждый вариант ответа уникальным
                    context.voteOptions.put(msg, 0); // добавляем опцию голосования, у которой пока что нет голосов
                }else{
                    ctx.writeAndFlush("Такой вариант ответа уже существует\nВведите другой вариант ответа " + (context.voteOptions.size() + 1) + "\n");
                }
                if(context.voteOptions.size() < context.optionsQuantity){
                    ctx.writeAndFlush("Введите вариант ответа " + (context.voteOptions.size() + 1) + "\n");
                }else{ // добавляем новый раздел, очищаем данные внутреннего класса
                    synchronized (Server.getTopics()) { // обеспечим потокобезопасность
                        Topic topic = Server.getTopics().get(context.currentTopic);
                        topic.addVote(new Vote(context.voteName, context.voteDescription, context.voteOptions, context.username));
                        ctx.writeAndFlush("Все варианты ответа записаны. Раздел голосования успешно создан\n");

                        context.currentState = CurrentState.MENU;
                        context.voteName = null;
                        context.voteDescription = null;
                        context.optionsQuantity = 0;
                        context.voteOptions.clear();
                    }
                }
                break;
        }
    }

    private void handleView(ChannelHandlerContext ctx, String[] messageParts){
        String topicName = null;
        String voteName = null;

        for(String part : messageParts){ // записываем входные параметры
            if(part.startsWith("-t=") && part.split("=").length == 2){
                topicName = part.split("=")[1];
            }else if(part.startsWith("-v=") && part.split("=").length == 2){
                voteName = part.split("=")[1];
            }
        }

        synchronized (Server.getTopics()) { // обеспечим потокобезопасность
            if (topicName != null && !Server.getTopics().containsKey(topicName)) { // проверка на отсутствие раздела
                ctx.writeAndFlush("Раздел с именем " + topicName + " не найден\n");
                return;
            }

            StringBuilder serverResponse = new StringBuilder();

            if (topicName == null && voteName == null) {// обработка команды view без параметров
                if (Server.getTopics().isEmpty()) {
                    serverResponse.append("Не создано ни одного раздела\n");
                } else {
                    serverResponse.append("Текущий список разделов:\n");
                    for (Map.Entry<String, Topic> topicEntry : Server.getTopics().entrySet()) { // достаем и выводим каждый существующий раздел
                        serverResponse.append(topicEntry.getKey()).append(" (голосований в разделе: ").append(topicEntry.getValue().getAllVotes().size()).append(")\n");
                    }
                    // логгировать, что пользователь запросил команду view
                }
            } else if (topicName != null && voteName == null) { // обработка команды с параметром -t
                Topic topic = Server.getTopics().get(topicName);
                serverResponse.append("Голосования в разделе: ").append(topicName).append(":\n");
                for (Map.Entry<String, Vote> voteEntry : topic.getAllVotes().entrySet()) {
                    serverResponse.append("- ").append(voteEntry.getKey()).append("\n");
                }
            } else if (topicName != null && voteName != null) { // обработка команды с параметрами -t, -v
                Topic topic = Server.getTopics().get(topicName);
                Vote vote = topic.getVote(voteName);
                if (vote == null) {
                    ctx.writeAndFlush("Голосование " + voteName + " не найдено в разделе " + topicName + "\n");
                    return;
                }
                serverResponse.append("Голосование ").append(voteName).append(":\n");
                serverResponse.append("Тема голосования: ").append(vote.getDescription()).append("\n").append("Варианты ответа:\n");
                for (Map.Entry<String, Integer> optionEntry : vote.getOptions().entrySet()) {
                    serverResponse.append("- ").append(optionEntry.getKey()).append(": ").append(optionEntry.getValue()).append(" голосов\n");
                }
            } else {
                ctx.writeAndFlush("Неверно введена команда view. Список доступных команд:\n view\n view -t=topic\n view -t=topic -v=vote\n");
                return;
            }

            ctx.writeAndFlush(serverResponse.toString());
        }
    }

    private void handleVote(ChannelHandlerContext ctx, String[] messageParts, ClientContext context){
        String topicName = null;
        String voteName = null;

        for (String part : messageParts) { // записываем входные параметры
            if (part.startsWith("-t=") && part.split("=").length == 2) {
                topicName = part.split("=")[1];
            } else if (part.startsWith("-v=") && part.split("=").length == 2) {
                voteName = part.split("=")[1];
            }
        }

        if (topicName == null) {
            ctx.writeAndFlush("Не указано имя раздела. Используйте параметр -t=topic\n");
            return;
        }
        synchronized (Server.getTopics()) { // обеспечим потокобезопасность
            if (!Server.getTopics().containsKey(topicName)) { // проверка на отсутствие раздела
                ctx.writeAndFlush("Раздел с именем " + topicName + " не найден\n");
                return;
            }

            Map<String, Vote> votes = Server.getTopics().get(topicName).getAllVotes();

            if (voteName == null) {
                ctx.writeAndFlush("Не указано имя голосования. Используйте параметр -v=vote\n");
                return;
            }

            if (votes.containsKey(voteName)) {
                StringBuilder serverResponse = new StringBuilder("Вы перешли к голосованию с именем ").append(voteName).append(". Содержание голосования:\n").append(votes.get(voteName).getDescription()).append("\n");
                serverResponse.append("Чтобы проголосовать, введите цифру, соответствующую вашему варианту ответа\n");

                Map<String, Integer> voteOptions = votes.get(voteName).getOptions();
                List<String> optionKeys = new ArrayList<>(voteOptions.keySet()); // помещаем ключи в список, чтобы мочь обращаться к опциям по индексу при обработке выбора

                for (int i = 0; i < optionKeys.size(); i++) {
                    serverResponse.append(i + 1).append(". ").append(optionKeys.get(i)).append("\n");
                }

                ctx.writeAndFlush(serverResponse.toString());

                context.currentState = CurrentState.WAITING_FOR_VOTE;
                context.voteName = voteName;
                context.currentTopic = topicName;
                context.currentOptions = optionKeys;
            } else {
                ctx.writeAndFlush("Голосования " + voteName + " не существует в разделе " + topicName + "\n");
                return;
            }
        }
    }

    private void handleVoteChoice(ChannelHandlerContext ctx, String msg, ClientContext context){
        try {
            int choice = Integer.parseInt(msg);

            if(choice < 0 || choice > context.currentOptions.size()){
                ctx.writeAndFlush("Ошибка ввода. Вы должны ввести число от 1 до " + context.currentOptions.size() + "\n");
                return;
            }

            synchronized (Server.getTopics()) { // обеспечим потокобезопасность
                String chosenOption = context.currentOptions.get(choice - 1);
                Topic topic = Server.getTopics().get(context.currentTopic);
                Vote vote = topic.getVote(context.voteName);
                vote.vote(chosenOption);

                ctx.writeAndFlush("Вы успешно проголосовали за вариант под номером " + chosenOption + "\n");
            }

            context.currentState = CurrentState.MENU;
            context.voteName = null;
            context.currentTopic = null;
            context.currentOptions = null;
        } catch (NumberFormatException e) {
            ctx.writeAndFlush("Ошибка ввода. Введите одно из доступных чисел\n");
        }
    }

    private void handleDelete(ChannelHandlerContext ctx, String[] messageParts, ClientContext context){
        String topicName = null;
        String voteName = null;

        for(String part : messageParts){ // записываем входные параметры
            if(part.startsWith("-t=") && part.split("=").length == 2){
                topicName = part.split("=")[1];
            }else if(part.startsWith("-v=") && part.split("=").length == 2){
                voteName = part.split("=")[1];
            }
        }

        if (topicName == null) {
            ctx.writeAndFlush("Не указано имя раздела. Используйте параметр -t=topic\n");
            return;
        }

        synchronized (Server.getTopics()) {
            if (!Server.getTopics().containsKey(topicName)) { // проверка на отсутствие раздела
                ctx.writeAndFlush("Раздел с именем " + topicName + " не найден\n");
                return;
            }

            Map<String, Vote> votes = Server.getTopics().get(topicName).getAllVotes();

            if (voteName == null) {
                ctx.writeAndFlush("Не указано имя голосования. Используйте параметр -v=vote\n");
                return;
            }
            if (votes.containsKey(voteName)) {
                if (context.username.equals(votes.get(voteName).getCreator())) {
                    Server.getTopics().get(topicName).deleteVote(voteName);
                    ctx.writeAndFlush("Голосование " + voteName + " было удалено из раздела " + topicName + "\n");
                } else {
                    ctx.writeAndFlush("Ошибка доступа. Вы можете удалять только созданные вами голосования");
                    return;
                }
            } else {
                ctx.writeAndFlush("Голосования " + voteName + " не существует в разделе " + topicName + "\n");
                return;
            }
        }
    }

    private void handleExit(ChannelHandlerContext ctx){
        ctx.writeAndFlush("Завершение работы\n").addListener(future -> {
            clientContexts.remove(ctx);
            ctx.close();
            //логгирование
        });
    }

    @Override
    protected  void channelRead0(ChannelHandlerContext ctx, String msg){
        ClientContext context = clientContexts.computeIfAbsent(ctx, key -> new ClientContext()); // получаем состояние, если оно уже есть, или записываем, если еще нет

        switch(context.currentState){
            case MENU:
                handleCommand(ctx, msg, context);
                break;
            case WAITING_FOR_VOTE:
                handleVoteChoice(ctx, msg, context);
                break;
            default:
                handleVoteCreation(ctx, msg, context);
                break;
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Клиент подключен: " + ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        cause.printStackTrace();
        //добавить логирование
        ctx.close();
    }
}

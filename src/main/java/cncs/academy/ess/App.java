package cncs.academy.ess;

import cncs.academy.ess.controller.AuthorizationMiddleware;
import cncs.academy.ess.controller.TodoController;
import cncs.academy.ess.controller.TodoListController;
import cncs.academy.ess.controller.UserController;
import cncs.academy.ess.repository.memory.InMemoryTodoListsRepository;
import cncs.academy.ess.repository.memory.InMemoryTodoRepository;
import cncs.academy.ess.repository.memory.InMemoryUserRepository;
import cncs.academy.ess.service.DuplicateUserException;
import cncs.academy.ess.service.TodoListsService;
import cncs.academy.ess.service.TodoService;
import cncs.academy.ess.service.TodoUserService;
import io.javalin.Javalin;

import java.security.NoSuchAlgorithmException;

public class App {

    public static void main(String[] args) throws Exception {

        // Arranque simples em HTTP, sem SSL
        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> it.anyHost());
            });
        });

        // Initialize repositories/services/controllers
        InMemoryUserRepository userRepository = new InMemoryUserRepository();
        TodoUserService userService = new TodoUserService(userRepository);
        UserController userController = new UserController(userService);

        InMemoryTodoListsRepository listsRepository = new InMemoryTodoListsRepository();
        TodoListsService toDoListService = new TodoListsService(listsRepository);
        TodoListController todoListController = new TodoListController(toDoListService);

        InMemoryTodoRepository todoRepository = new InMemoryTodoRepository();
        TodoService todoService = new TodoService(todoRepository, listsRepository);
        TodoController todoController = new TodoController(todoService, toDoListService);

        AuthorizationMiddleware authMiddleware = new AuthorizationMiddleware(userRepository);

        // CORS headers
        app.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "*");
        });

        // Responder a preflight OPTIONS sem exigir autenticação
        app.options("/*", ctx -> ctx.status(200));

        // Authorization middleware
        app.before(ctx -> {
            if ("OPTIONS".equalsIgnoreCase(ctx.method().name())) {
                return;
            }
            authMiddleware.handle(ctx);
        });

        // User management
        app.post("/user", userController::createUser);
        app.get("/user/{userId}", userController::getUser);
        app.delete("/user/{userId}", userController::deleteUser);
        app.post("/login", userController::loginUser);

        // "To do" lists management
        app.post("/todolist", todoListController::createTodoList);
        app.get("/todolist", todoListController::getAllTodoLists);
        app.get("/todolist/{listId}", todoListController::getTodoList);

        // "To do" list items management
        app.post("/todo/item", todoController::createTodoItem);
        app.get("/todo/{listId}/tasks", todoController::getAllTodoItems);
        app.get("/todo/{listId}/tasks/{taskId}", todoController::getTodoItem);
        app.delete("/todo/{listId}/tasks/{taskId}", todoController::deleteTodoItem);

        fillDummyData(userService, toDoListService, todoService);

        // Arrancar explicitamente na porta 8080
        app.start(8080);
    }

    private static void fillDummyData(
            TodoUserService userService,
            TodoListsService toDoListService,
            TodoService todoService) throws NoSuchAlgorithmException, DuplicateUserException {

        userService.addUser("user1", "password1");
        userService.addUser("user2", "password2");
        userService.addUser("user3", "password3");
        userService.addUser("user69", "password69");

        toDoListService.createTodoListItem("Shopping list", 1);
        toDoListService.createTodoListItem("Other", 1);
        toDoListService.createTodoListItem("Shopping list2", 2);
        toDoListService.createTodoListItem("Shopping list3", 2);
        toDoListService.createTodoListItem("Other", 2);
        toDoListService.createTodoListItem("Other", 3);
        toDoListService.createTodoListItem("Shopping list4", 3);
        toDoListService.createTodoListItem("Shopping list5", 3);
        toDoListService.createTodoListItem("Other1", 3);
        toDoListService.createTodoListItem("Other2", 3);

        todoService.createTodoItem("Bread", 1);
        todoService.createTodoItem("Milk", 1);
        todoService.createTodoItem("Eggs", 1);
        todoService.createTodoItem("Cheese", 1);
        todoService.createTodoItem("Butter", 1);
    }
}
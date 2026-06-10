package net.marcelomartins.maisumtodo.api;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import net.marcelomartins.maisumtodo.domain.SystemLogin;
import net.marcelomartins.maisumtodo.domain.TaskStatus;
import net.marcelomartins.maisumtodo.domain.TodoProject;
import net.marcelomartins.maisumtodo.domain.TodoTask;
import net.marcelomartins.maisumtodo.infra.UserRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class TodoResource {

    @Inject
    UserRequest userRequest;

    @GET
    @Path("/projects")
    public List<ProjectResponse> listProjects() {
        SystemLogin login = userRequest.getSystemLogin();
        List<TodoProject> projects = TodoProject.find("systemLogin = ?1 order by dateCreated asc", login).list();
        return projects.stream().map(this::toProjectResponse).toList();
    }

    @POST
    @Path("/projects")
    public Response createProject(ProjectRequest request) {
        SystemLogin login = userRequest.getSystemLogin();
        String name = normalizeRequired(request == null ? null : request.name(), "Project name is required");

        TodoProject project = new TodoProject();
        project.systemLogin = login;
        project.name = name;
        project.persist();

        return Response.status(Response.Status.CREATED)
                .entity(toProjectResponse(project))
                .build();
    }

    @PUT
    @Path("/projects/{projectUuid}")
    public ProjectResponse updateProject(@PathParam("projectUuid") String projectUuid, ProjectRequest request) {
        TodoProject project = findProject(projectUuid, userRequest.getSystemLogin());
        project.name = normalizeRequired(request == null ? null : request.name(), "Project name is required");
        project.persist();
        return toProjectResponse(project);
    }

    @DELETE
    @Path("/projects/{projectUuid}")
    public Response deleteProject(@PathParam("projectUuid") String projectUuid) {
        TodoProject project = findProject(projectUuid, userRequest.getSystemLogin());
        project.delete();
        return Response.noContent().build();
    }

    @GET
    @Path("/projects/{projectUuid}/tasks")
    public List<TaskResponse> listTasks(@PathParam("projectUuid") String projectUuid) {
        TodoProject project = findProject(projectUuid, userRequest.getSystemLogin());
        List<TodoTask> tasks = TodoTask.find("project = ?1 order by sortOrder asc, dateCreated asc", project).list();
        return tasks.stream().map(this::toTaskResponse).toList();
    }

    @POST
    @Path("/projects/{projectUuid}/tasks")
    public Response createTask(@PathParam("projectUuid") String projectUuid, TaskRequest request) {
        TodoProject project = findProject(projectUuid, userRequest.getSystemLogin());
        String title = normalizeRequired(request == null ? null : request.title(), "Task title is required");

        TodoTask task = new TodoTask();
        task.project = project;
        task.title = title;
        task.status = request != null && request.status() != null ? request.status() : TaskStatus.TODO;
        task.sortOrder = request != null && request.sortOrder() != null ? request.sortOrder() : nextSortOrder(project);
        task.persist();

        return Response.status(Response.Status.CREATED)
                .entity(toTaskResponse(task))
                .build();
    }

    @PUT
    @Path("/tasks/{taskUuid}")
    public TaskResponse updateTask(@PathParam("taskUuid") String taskUuid, TaskRequest request) {
        if (request == null) {
            throw new BadRequestException("Task request is required");
        }

        TodoTask task = findTask(taskUuid, userRequest.getSystemLogin());
        if (request.title() != null) {
            task.title = normalizeRequired(request.title(), "Task title is required");
        }
        if (request.status() != null) {
            task.status = request.status();
        }
        if (request.sortOrder() != null) {
            task.sortOrder = request.sortOrder();
        }
        task.persist();
        return toTaskResponse(task);
    }

    @DELETE
    @Path("/tasks/{taskUuid}")
    public Response deleteTask(@PathParam("taskUuid") String taskUuid) {
        TodoTask task = findTask(taskUuid, userRequest.getSystemLogin());
        task.delete();
        return Response.noContent().build();
    }

    private TodoProject findProject(String projectUuid, SystemLogin login) {
        TodoProject project = TodoProject.find("uuid = ?1 and systemLogin = ?2", projectUuid, login).firstResult();
        if (project == null) {
            throw new NotFoundException();
        }
        return project;
    }

    private TodoTask findTask(String taskUuid, SystemLogin login) {
        TodoTask task = TodoTask.find("uuid = ?1 and project.systemLogin = ?2", taskUuid, login).firstResult();
        if (task == null) {
            throw new NotFoundException();
        }
        return task;
    }

    private BigDecimal nextSortOrder(TodoProject project) {
        return BigDecimal.valueOf(TodoTask.count("project = ?1", project) + 1L);
    }

    private String normalizeRequired(String value, String message) {
        String normalized = value == null ? null : value.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new BadRequestException(message);
        }
        return normalized;
    }

    private ProjectResponse toProjectResponse(TodoProject project) {
        return new ProjectResponse(project.uuid, project.name, project.dateCreated, project.lastUpdated);
    }

    private TaskResponse toTaskResponse(TodoTask task) {
        return new TaskResponse(
                task.uuid,
                task.project.uuid,
                task.title,
                task.status,
                task.sortOrder,
                task.dateCreated,
                task.lastUpdated);
    }

    public record ProjectRequest(String name) {
    }

    public record ProjectResponse(String uuid, String name, LocalDateTime dateCreated, LocalDateTime lastUpdated) {
    }

    public record TaskRequest(String title, TaskStatus status, BigDecimal sortOrder) {
    }

    public record TaskResponse(
            String uuid,
            String projectUuid,
            String title,
            TaskStatus status,
            BigDecimal sortOrder,
            LocalDateTime dateCreated,
            LocalDateTime lastUpdated) {
    }
}

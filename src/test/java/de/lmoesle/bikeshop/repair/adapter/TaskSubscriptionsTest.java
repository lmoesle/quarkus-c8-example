package de.lmoesle.bikeshop.repair.adapter;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import de.lmoesle.bikeshop.repair.adapter.in.subscription.TaskSubscriptions;
import de.lmoesle.bikeshop.repair.application.ports.out.TaskProjection;
import dev.bpmcrafters.processengineapi.task.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TaskSubscriptionsTest {
  @Test
  void clearsStaleRowsBeforeSubscribingAndDeletesUsingOnlySuppliedTaskId() {
    final var subscriptions = mock(TaskSubscriptionApi.class);
    final var tasks = mock(TaskProjection.class);
    when(subscriptions.subscribeForTask(any())).thenReturn(CompletableFuture.completedFuture(null));
    final var adapter = new TaskSubscriptions(subscriptions, tasks);
    adapter.start(null);
    final var command = ArgumentCaptor.forClass(SubscribeForTaskCmd.class);
    final var order = inOrder(tasks, subscriptions);
    order.verify(tasks).reset();
    order.verify(subscriptions).subscribeForTask(command.capture());
    assertThat(command.getValue().getPayloadDescription()).isNull();
    command
        .getValue()
        .getTermination()
        .accept(new TaskInformation("existing-id", Map.of("reason", "delete")));
    verify(tasks).delete("existing-id");
    final var payload = new HashMap<String, Object>();
    payload.put("repairId", "repair");
    payload.put("nullable", null);
    final var task =
        new TaskInformation(
            "new-id",
            Map.of(
                "processInstanceId",
                "123",
                "activityId",
                "ReviewOffer",
                "candidateGroups",
                "customer-service"));
    command.getValue().getAction().accept(task, payload);
    command.getValue().getAction().accept(task, payload);
    verify(tasks, times(2))
        .upsert(
            argThat(t ->
                t.id().equals("new-id")
                    && t.payload().containsKey("nullable")
                    && t.candidateGroups().contains("customer-service")));
    verify(tasks, never()).delete("new-id");
  }
}

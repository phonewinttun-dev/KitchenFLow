package com.anyawalker.poskds.features.order;

import com.anyawalker.poskds.features.order.dtos.OrderResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class OrderListenerService {
    //for cashier only
    Map<Long, List<DeferredResult<@NonNull OrderResponse>>> listeners = new ConcurrentHashMap<>();
    //for chef and admin

    //add the incoming request to wait
    public void register(Long userId,DeferredResult<@NonNull OrderResponse>  listener){
        //register the incoming request with userId
        listeners.computeIfAbsent(userId,k -> new CopyOnWriteArrayList<>()).add(listener);

        listener.onCompletion(() -> removeListener(userId,listener));
        listener.onTimeout(() -> removeListener(userId, listener));
        listener.onError(t -> removeListener(userId, listener));


    }
    public void removeListener(Long userId,DeferredResult<@NonNull OrderResponse> listener){
        listeners.computeIfPresent(userId, (key,list) -> {
            list.remove(listener);
            return list.isEmpty() ? null : list;
        });
    }

    public void resolveListener(Long userId,OrderResponse completedOrderResponse){
        //.compute has the thread safe feature and easy to write but
        //null mean delete and returning the same value mean not delete from Map<>
        listeners.computeIfPresent(userId,(key,list) -> {
           for (DeferredResult<@NonNull OrderResponse> awaitingListener : list){
               //this is like a doFilter() the releasing point
               awaitingListener.setResult(completedOrderResponse);
           }
           return null;
        });
    }
}

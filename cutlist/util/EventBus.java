package cutlist.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * https://www.vogella.com/tutorials/DesignPatternSingleton/article.html
 */
public enum EventBus {
    INSTANCE;

    public enum Event {
        TEMP,

        MOUSE_ENTERED_BOARD,
        MOUSE_EXITED_BOARD,
        MOUSE_ENTERED_CUT_PIECE,
        MOUSE_EXITED_CUT_PIECE,
        DEMAND_SELECTED,
        DEMAND_DESELECTED,
        STOCK_SELECTED,
        STOCK_DESELECTED,
        CUT_SELECTED,
        CUT_DESELECTED,
        DEMAND_CUT,
        DEMAND_UNCUT;
    }

    public static interface Listener {
        void eventTriggered(Event event, Object source);
    }

    protected static record ListenerWithContext(Listener listener, Object context) {};

    protected Map<Event, Set<ListenerWithContext>> m_allListeners = new HashMap<>();;

    public void addListener(Event event, Listener listener) {
        addListener(event, listener, null);
    }

    /**
     * Only fires this even for this listener if the Object passed in the trigger matches the context object.
     */
    public void addListener(Event event, Listener listener, Object context) {
        var listeners = m_allListeners.get(event);

        if (listeners == null) {
            listeners = new HashSet<>();

            m_allListeners.put(event, listeners);
        }

        listeners.add(new ListenerWithContext(listener, context));
    }

    public void addListener(List<Event> events, Listener listener) {
        addListener(events, listener, null);
    }

    public void addListener(List<Event> events, Listener listener, Object context) {
        events.forEach(event -> addListener(event, listener, context));
    }

    public int triggerEvent(Event event, Object source) {
        var listeners = m_allListeners.get(event);

        if (listeners == null) {
            return 0;
        }

        listeners.stream()
            .filter(listenerWithContext -> listenerWithContext.context() == null || listenerWithContext.context() == source)
            .forEach(listenerWithContext -> listenerWithContext.listener().eventTriggered(event, source));

        return listeners.size();
    }
}

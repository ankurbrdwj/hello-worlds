package com.ankur.spark;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Parent/child bookkeeping shared by {@link EventType} implementations. */
public class Delegator {

    private final EventType parent;
    private final List<EventType> children = new ArrayList<>();

    public Delegator(EventType parent, EventType self) {
        this.parent = parent;
        if (parent != null) {
            parent.addChild(self);
        }
    }

    public EventType getParent() {
        return parent;
    }

    public void addChild(EventType child) {
        children.add(child);
    }

    public List<EventType> getChildren() {
        return children;
    }

    public static Set<EventType> getRootEvents(EventType[] events) {
        Set<EventType> roots = new LinkedHashSet<>();
        for (EventType event : events) {
            if (event.getParent() == null) {
                roots.add(event);
            }
        }
        return roots;
    }
}

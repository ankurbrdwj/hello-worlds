package com.ankur.spark;

import java.util.List;

public interface EventType {

    EventType getParent();

    void addChild(EventType child);

    List<EventType> getChildren();
}

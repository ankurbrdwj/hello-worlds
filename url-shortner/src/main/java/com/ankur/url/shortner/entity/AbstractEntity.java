package com.ankur.url.shortner.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@MappedSuperclass
@Getter
@Setter
public class AbstractEntity implements Serializable {

    protected static final int DEFAULT_SCALE = 2;
    private static final long serialVersionUID = -3733796182675284472L;
    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected long id = 0;
    /**
     * Auto-incrementing version (integer) column.  Incremented automatically
     * on each UPDATE.  Can be used for optimistic locking.
     */
    @Version
    int version = 0;
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractEntity)) return false;
        AbstractEntity that = (AbstractEntity) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}

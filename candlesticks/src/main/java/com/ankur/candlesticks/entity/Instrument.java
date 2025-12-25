package com.ankur.candlesticks.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder(builderClassName = "Builder", setterPrefix = "set")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "instruments")
public class Instrument extends AbstractEntity {

  @Column(unique = true)
  String isin;

  String description;

  @Column(nullable = false)
  @Builder.Default
  boolean active = true;

  Instant deletedAt;

  @JsonIgnore
  @OneToMany(mappedBy = "instrument", cascade = CascadeType.REMOVE, orphanRemoval = true)
  List<Quote> quotes;
}

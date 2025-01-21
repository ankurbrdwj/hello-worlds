package com.ankur.candlesticks.entity;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Builder(builderClassName = "Builder", setterPrefix = "set")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "instruments")
public class Instrument extends  AbstractEntity{

  String isin;
  String description;

  @OneToMany(mappedBy = "instrument", cascade = CascadeType.REMOVE, orphanRemoval = true)
  List<Quote> quotes;
}

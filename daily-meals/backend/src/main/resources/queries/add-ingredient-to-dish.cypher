// Create relationships between the Dish and Ingredients
MATCH (d:Dish {dishName: 'Paneer Butter Masala'}),
      (i1:Ingredient {nameInEnglish: 'Paneer'}),
      (i2:Ingredient {nameInEnglish: 'Butter'}),
      (i3:Ingredient {nameInEnglish: 'Tomato'})
CREATE (d)-[:CONTAINS]->(i1),
       (d)-[:CONTAINS]->(i2),
       (d)-[:CONTAINS]->(i3);

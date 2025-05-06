// Create Ingredient nodes with properties
CREATE (i1:Ingredient {
  nameInEnglish: 'Paneer',
  nameInHindi: 'पनीर',
  nameInGerman: 'Käse'
}),
       (i2:Ingredient {
         nameInEnglish: 'Butter',
         nameInHindi: 'मक्खन',
         nameInGerman: 'Butter'
       }),
       (i3:Ingredient {
         nameInEnglish: 'Tomato',
         nameInHindi: 'टमाटर',
         nameInGerman: 'Tomate'
       });

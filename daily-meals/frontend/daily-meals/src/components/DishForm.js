import React, { useState } from "react";
import { Mic, MicOff, UtensilsCrossed, Leaf } from "lucide-react";

function DishForm() {
  const [dishName, setDishName] = useState("");
  const [ingredients, setIngredients] = useState("");
  const [spices, setSpices] = useState("");
  const [dishType, setDishType] = useState("MainCourse");
  const [isListening, setIsListening] = useState(false);
  const [activeField, setActiveField] = useState(null);

  const startListening = (field, setter) => {
    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
      const SpeechRecognition = window.webkitSpeechRecognition || window.SpeechRecognition;
      const recognition = new SpeechRecognition();
      recognition.continuous = false;
      recognition.interimResults = false;

      recognition.onstart = () => {
        setIsListening(true);
        setActiveField(field);
      };

      recognition.onresult = (event) => {
        const transcript = event.results[0][0].transcript;
        setter(transcript);
      };

      recognition.onend = () => {
        setIsListening(false);
        setActiveField(null);
      };

      recognition.start();
    } else {
      alert('Speech recognition is not supported in this browser.');
    }
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    console.log({ dishName, ingredients, spices, dishType });
  };

  const InputWithVoice = ({ value, onChange, placeholder, type = "text", multiline = false }) => (
    <div className="relative flex items-start">
      {multiline ? (
        <textarea
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          className="w-full p-2 border rounded-md min-h-[100px] pr-10 bg-green-50 border-green-200 focus:outline-none focus:ring-2 focus:ring-green-300"
        />
      ) : (
        <input
          type={type}
          value={value}
          onChange={onChange}
          placeholder={placeholder}
          className="w-full p-2 border rounded-md pr-10 bg-green-50 border-green-200 focus:outline-none focus:ring-2 focus:ring-green-300"
        />
      )}
      <button
        type="button"
        onClick={() => startListening(placeholder, onChange.bind(null))}
        className={`absolute right-2 top-2 p-1 rounded-full
          ${activeField === placeholder && isListening
            ? 'text-red-500 animate-pulse'
            : 'text-green-600 hover:text-green-700'}`}
      >
        {activeField === placeholder && isListening ? <MicOff size={20} /> : <Mic size={20} />}
      </button>
    </div>
  );

  return (
    <div className="max-w-2xl mx-auto bg-white shadow-lg rounded-lg overflow-hidden">
      <div className="bg-green-100 p-4 flex items-center justify-center gap-2">
        <UtensilsCrossed className="text-green-600" />
        <h1 className="text-2xl font-bold text-green-800">आज क्या बनेगा? (Aaj Kya Banega?)</h1>
        <Leaf className="text-green-600" />
      </div>
      <div className="p-6">
        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="space-y-2">
            <label className="block font-medium text-green-800">Name of Dish:</label>
            <InputWithVoice
              value={dishName}
              onChange={(e) => setDishName(typeof e === 'string' ? e : e.target.value)}
              placeholder="Enter dish name"
            />
          </div>

          <div className="space-y-2">
            <label className="block font-medium text-green-800">List of Ingredients:</label>
            <InputWithVoice
              value={ingredients}
              onChange={(e) => setIngredients(typeof e === 'string' ? e : e.target.value)}
              placeholder="Enter ingredients"
              multiline
            />
          </div>

          <div className="space-y-2">
            <label className="block font-medium text-green-800">List of Spices:</label>
            <InputWithVoice
              value={spices}
              onChange={(e) => setSpices(typeof e === 'string' ? e : e.target.value)}
              placeholder="Enter spices"
              multiline
            />
          </div>

          <div className="space-y-2">
            <label className="block font-medium text-green-800">Dish Type:</label>
            <select
              value={dishType}
              onChange={(e) => setDishType(e.target.value)}
              className="w-full p-2 border rounded-md bg-green-50 border-green-200 focus:outline-none focus:ring-2 focus:ring-green-300"
            >
              <option value="Beverage">Beverage</option>
              <option value="Starter">Starter</option>
              <option value="MainCourse">Main Course</option>
              <option value="Bread">Bread</option>
              <option value="Condiment">Condiment</option>
              <option value="Snack">Snack</option>
              <option value="Dessert">Dessert</option>
            </select>
          </div>

          <button
            type="submit"
            className="w-full bg-green-600 text-white py-3 px-4 rounded-md hover:bg-green-700 transition-colors flex items-center justify-center gap-2 shadow-md"
          >
            <UtensilsCrossed size={20} />
            Save Dish
          </button>
        </form>
      </div>
    </div>
  );
}

export default DishForm;

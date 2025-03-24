import { useState } from "react";
import "./App.scss";
import { Button, HeaderMenuButton } from "@carbon/react";

function App() {
  const [count, setCount] = useState(0);

  return (
    <>
      <HeaderMenuButton></HeaderMenuButton>
    </>
  );
}

export default App;

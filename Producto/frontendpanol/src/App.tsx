import { InventoryCategoriesPage } from "./pages/InventoryCategoriesPage";
import { InventoryItemsPage } from "./pages/InventoryItemsPage";
import { useEffect, useState } from "react";

function App() {
  const [hash, setHash] = useState(() => window.location.hash);

  useEffect(() => {
    function handleHashChange() {
      setHash(window.location.hash);
    }

    window.addEventListener("hashchange", handleHashChange);
    return () => window.removeEventListener("hashchange", handleHashChange);
  }, []);

  const normalizedHash = hash || "#/inventory/categories";

  if (
    normalizedHash.startsWith("#/inventory/implementos") ||
    normalizedHash.startsWith("#/inventory/items")
  ) {
    return <InventoryItemsPage />;
  }

  return <InventoryCategoriesPage />;
}

export default App;


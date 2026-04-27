import { Bell, Boxes, ClipboardList, ClipboardPlus, MapPin, PackageSearch, Settings } from "lucide-react";
import type { ReactNode } from "react";

const menuInventory = [
  { key: "summary", label: "Resumen", icon: PackageSearch, href: "#/inventory" },
  { key: "items", label: "Implementos", icon: Boxes, href: "#/inventory/implementos" },
  { key: "categories", label: "Categorias", icon: ClipboardList, href: "#/inventory/categories" },
  { key: "locations", label: "Ubicaciones", icon: MapPin, href: "#/inventory/locations" },
  { key: "moves", label: "Movimientos", icon: ClipboardPlus, href: "#/inventory/moves" },
] as const;

const menuConfig = [
  { label: "Unidades de medida", icon: Settings },
  { label: "Atributos de items", icon: Settings },
  { label: "Historial de cambios", icon: Settings },
];

export type InventorySection = (typeof menuInventory)[number]["key"];

export function Sidebar({ activeSection }: { activeSection: InventorySection }) {
  return (
    <aside className="sidebar">
      <section>
        <h3 className="sidebar__title">Inventario</h3>
        <ul className="sidebar__list">
          {menuInventory.map((item) => {
            const Icon = item.icon;
            const isActive = item.key === activeSection;
            return (
              <li key={item.label}>
                <a
                  href={item.href}
                  className={isActive ? "sidebar__item sidebar__item--active" : "sidebar__item"}
                >
                  <Icon size={18} />
                  <span>{item.label}</span>
                </a>
              </li>
            );
          })}
        </ul>
      </section>

      <section>
        <h3 className="sidebar__title">Configuracion</h3>
        <ul className="sidebar__list">
          {menuConfig.map((item) => {
            const Icon = item.icon;
            return (
              <li key={item.label} className="sidebar__item">
                <Icon size={18} />
                <span>{item.label}</span>
              </li>
            );
          })}
        </ul>
      </section>

      <section className="sidebar__help">
        <h4>Necesitas ayuda?</h4>
        <p>Revisa la guia rapida de inventario para coordinadores.</p>
        <button type="button" className="button button--ghost">
          Ver guia
        </button>
      </section>
    </aside>
  );
}

export function TopBar() {
  return (
    <header className="topbar">
      <div className="topbar__brand">
        <div className="topbar__logo">P</div>
        <div>
          <strong>Panolero</strong>
          <p>Escuela de Salud</p>
        </div>
      </div>

      <nav className="topbar__menu">
        <a href="#">Inicio</a>
        <a href="#" className="is-active">
          Inventario
        </a>
        <a href="#">Prestamos</a>
        <a href="#">Reportes</a>
      </nav>

      <div className="topbar__user">
        <button type="button" className="topbar__icon">
          <Bell size={18} />
        </button>
        <div className="topbar__avatar">FJ</div>
        <div>
          <strong>Francisco Jimenez</strong>
          <p>Coordinador de Laboratorio</p>
        </div>
      </div>
    </header>
  );
}

export function InventoryLayout({
  children,
  activeSection = "categories",
}: {
  children: ReactNode;
  activeSection?: InventorySection;
}) {
  return (
    <div className="app-shell">
      <TopBar />
      <div className="app-shell__content">
        <Sidebar activeSection={activeSection} />
        <main className="app-shell__main">{children}</main>
      </div>
    </div>
  );
}

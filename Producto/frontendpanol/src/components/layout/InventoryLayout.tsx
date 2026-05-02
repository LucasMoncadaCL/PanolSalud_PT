import {
  Bell,
  Boxes,
  ClipboardList,
  FileBarChart2,
  Handshake,
  MapPin,
  Menu,
  Search,
  X,
} from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import type { KeyboardEvent } from "react";
import type { ReactNode } from "react";
import { fetchImplements } from "../../services/implementService";
import type { ImplementSummary } from "../../types/implement";

const menuInventory = [
  { key: "items", label: "Implementos", icon: Boxes, href: "#/inventory/implementos" },
  { key: "categories", label: "Categorias", icon: ClipboardList, href: "#/inventory/categories" },
  { key: "locations", label: "Ubicaciones", icon: MapPin, href: "#/inventory/locations" },
  { key: "moves", label: "Movimientos", icon: ClipboardList, href: "#/inventory/moves" },
  { key: "loans", label: "Prestamos", icon: Handshake, href: "#/inventory/implementos" },
  { key: "reports", label: "Reportes", icon: FileBarChart2, href: "#/inventory/implementos" },
] as const;

export type InventorySection = "items" | "categories" | "locations" | "moves" | "loans" | "reports";
export interface BreadcrumbPart {
  label: string;
  href?: string;
}

export function Sidebar({
  activeSection,
  onNavigate,
}: {
  activeSection: InventorySection;
  onNavigate?: () => void;
}) {
  return (
    <aside className="sidebar">
      <div className="sidebar__brand">
        <div>
          <strong>Coordinador de laboratorio</strong>
          <p>Escuela de salud</p>
        </div>
      </div>

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
                  onClick={onNavigate}
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

export function TopBar({
  sidebarOpen,
  onToggleSidebar,
  breadcrumbs,
}: {
  sidebarOpen: boolean;
  onToggleSidebar: () => void;
  breadcrumbs: BreadcrumbPart[];
}) {
  const [search, setSearch] = useState("");
  const [debouncedSearch, setDebouncedSearch] = useState("");
  const [suggestions, setSuggestions] = useState<ImplementSummary[]>([]);
  const [loadingSuggestions, setLoadingSuggestions] = useState(false);
  const [suggestionsOpen, setSuggestionsOpen] = useState(false);
  const [hoverIndex, setHoverIndex] = useState<number>(-1);
  const searchRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const timeout = window.setTimeout(() => setDebouncedSearch(search.trim()), 250);
    return () => window.clearTimeout(timeout);
  }, [search]);

  useEffect(() => {
    if (debouncedSearch.length < 2) {
      setSuggestions([]);
      setLoadingSuggestions(false);
      setHoverIndex(-1);
      return;
    }

    let cancelled = false;
    setLoadingSuggestions(true);
    fetchImplements({ name: debouncedSearch })
      .then((rows) => {
        if (cancelled) return;
        setSuggestions(rows.slice(0, 8));
      })
      .catch(() => {
        if (cancelled) return;
        setSuggestions([]);
      })
      .finally(() => {
        if (cancelled) return;
        setLoadingSuggestions(false);
      });

    return () => {
      cancelled = true;
    };
  }, [debouncedSearch]);

  useEffect(() => {
    function onClickOutside(event: MouseEvent) {
      if (!searchRef.current) return;
      if (!searchRef.current.contains(event.target as Node)) {
        setSuggestionsOpen(false);
      }
    }

    document.addEventListener("mousedown", onClickOutside);
    return () => document.removeEventListener("mousedown", onClickOutside);
  }, []);

  const shouldShowSuggestions = useMemo(
    () => suggestionsOpen && search.trim().length >= 2,
    [search, suggestionsOpen],
  );

  function goToImplement(row: ImplementSummary) {
    window.location.hash = `#/inventory/implementos/${row.id}`;
    setSuggestionsOpen(false);
    setSearch("");
    setHoverIndex(-1);
  }

  function handleSearchKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (!shouldShowSuggestions) return;
    if (event.key === "ArrowDown") {
      event.preventDefault();
      setHoverIndex((prev) => Math.min(prev + 1, suggestions.length - 1));
      return;
    }
    if (event.key === "ArrowUp") {
      event.preventDefault();
      setHoverIndex((prev) => Math.max(prev - 1, 0));
      return;
    }
    if (event.key === "Enter" && suggestions.length > 0) {
      event.preventDefault();
      const target = hoverIndex >= 0 ? suggestions[hoverIndex] : suggestions[0];
      goToImplement(target);
      return;
    }
    if (event.key === "Escape") {
      setSuggestionsOpen(false);
      setHoverIndex(-1);
    }
  }

  return (
    <header className="topbar">
      <button
        type="button"
        className="topbar__burger topbar__burger--inline"
        onClick={onToggleSidebar}
        aria-label="Toggle menu lateral"
      >
        {sidebarOpen ? <X size={18} /> : <Menu size={18} />}
      </button>

      <nav className="topbar__crumbs" aria-label="Ruta actual">
        {breadcrumbs.map((part, index) => {
          const isLast = index === breadcrumbs.length - 1;
          return (
            <span key={`${part.label}-${index}`} className="topbar__crumb-item">
              {part.href && !isLast ? (
                <a href={part.href} className="topbar__crumb-link">
                  {part.label}
                </a>
              ) : (
                <span className={isLast ? "topbar__crumb-current" : "topbar__crumb-link"}>
                  {part.label}
                </span>
              )}
              {!isLast ? <span className="topbar__crumb-sep">/</span> : null}
            </span>
          );
        })}
      </nav>

      <div className="topbar__search" ref={searchRef}>
        <Search size={16} />
        <input
          type="search"
          placeholder="Buscar implementos..."
          value={search}
          onChange={(event) => {
            setSearch(event.target.value);
            setSuggestionsOpen(true);
            setHoverIndex(-1);
          }}
          onFocus={() => setSuggestionsOpen(true)}
          onKeyDown={handleSearchKeyDown}
        />
        {shouldShowSuggestions ? (
          <div className="topbar-search-suggest">
            {loadingSuggestions ? (
              <div className="topbar-search-suggest__hint">Buscando implementos...</div>
            ) : suggestions.length === 0 ? (
              <div className="topbar-search-suggest__hint">Sin coincidencias</div>
            ) : (
              suggestions.map((row, index) => (
                <button
                  key={row.id}
                  type="button"
                  className={`topbar-search-item ${index === hoverIndex ? "is-hover" : ""}`}
                  onMouseEnter={() => setHoverIndex(index)}
                  onClick={() => goToImplement(row)}
                >
                  <img
                    src={(row.imgUrl ?? (row as any).img_url) ?? "https://placehold.co/48x48/e9edf5/4d6284?text=Sin+img"}
                    alt={row.name}
                    className="topbar-search-item__thumb"
                  />
                  <span className="topbar-search-item__name">{row.name}</span>
                </button>
              ))
            )}
          </div>
        ) : null}
      </div>

      <div className="topbar__user">
        <button type="button" className="topbar__icon" aria-label="Notificaciones">
          <Bell size={18} />
        </button>
        <div className="topbar__avatar">FJ</div>
        <div>
          <strong>Francisco Jimenez</strong>
          <p>Coordinador de laboratorio</p>
        </div>
      </div>
    </header>
  );
}

export function InventoryLayout({
  children,
  activeSection = "categories",
  breadcrumbs = [{ label: "Inventario", href: "#/inventory/implementos" }],
}: {
  children: ReactNode;
  activeSection?: InventorySection;
  breadcrumbs?: BreadcrumbPart[];
}) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  useEffect(() => {
    function onResize() {
      if (window.innerWidth > 1100) {
        setSidebarOpen(false);
      }
    }

    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  function closeSidebarOnNavigate() {
    if (window.innerWidth <= 1100) {
      setSidebarOpen(false);
    }
  }

  return (
    <div className={`app-shell ${sidebarOpen ? "app-shell--sidebar-open" : ""}`}>
      <Sidebar activeSection={activeSection} onNavigate={closeSidebarOnNavigate} />
      <div className="app-shell__workspace">
        <TopBar
          sidebarOpen={sidebarOpen}
          onToggleSidebar={() => setSidebarOpen((value) => !value)}
          breadcrumbs={breadcrumbs}
        />
        <main className="app-shell__main">{children}</main>
      </div>
      {sidebarOpen ? (
        <button
          type="button"
          className="sidebar-overlay"
          aria-label="Cerrar menu lateral"
          onClick={() => setSidebarOpen(false)}
        />
      ) : null}
    </div>
  );
}


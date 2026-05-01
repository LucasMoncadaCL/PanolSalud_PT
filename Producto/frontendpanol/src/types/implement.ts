export interface InventoryMovementDetail {
  id: string;
  action: string;
  quantity: number;
  timestamp: string;
  notes: string | null;
  implement_id: number;
  performed_by: string;
}

export interface ImplementCreatePayload {
  name: string;
  category_id: number;
  item_type: "consumable" | "reusable" | "individual";
  location_id: number;
  description: string | null;
  barcode: string | null;
  img_url: string | null;
  min_stock: number;
  observations: string | null;
}

export interface ImplementUpdatePayload {
  name: string;
  category_id: number | null;
  location_id: number;
  item_type: "consumable" | "reusable" | "individual";
  description: string | null;
  barcode: string | null;
  img_url: string | null;
  min_stock: number;
  observations: string | null;
}

export interface ImplementSummary {
  id: number;
  name: string;
  description?: string | null;
  barcode?: string | null;
  imgUrl?: string | null;
  active?: boolean;
  available?: boolean;
  category: {
    id: number;
    name: string;
    active: boolean;
  } | null;
  location: {
    id: number;
    name: string;
    description: string | null;
  } | null;
  stock?: {
    total_stock?: number | null;
    min_stock?: number | null;
    available?: number | null;
    reserved?: number | null;
    loaned?: number | null;
    damaged?: number | null;
    available_display?: string | null;
  } | null;
}

export type ImplementStockFilterStatus =
  | "all"
  | "available"
  | "reserved"
  | "loaned"
  | "damaged"
  | "blocked";

export const STOCK_STATUS_LABELS: Record<ImplementStockFilterStatus, string> = {
  all: "Todos los estados",
  available: "Disponible",
  reserved: "Reservado",
  loaned: "Prestado",
  damaged: "Dañado",
  blocked: "Bloqueado",
};

export interface ImplementFilters {
  name?: string;
  categoryId?: number | null;
  stockStatus?: ImplementStockFilterStatus;
}

export interface ImplementDetail {
  id: number;
  name: string;
  description: string | null;
  item_type: "consumable" | "reusable" | "individual" | null;
  display_location?: string | null;
  category: {
    id: number;
    name: string;
    active: boolean;
  } | null;
  location: {
    id: number;
    name: string;
    description: string | null;
  } | null;
  categoryId: number | null;
  locationId: number | null;
  min_stock: number | null;
  barcode: string | null;
  img_url: string | null;
  observations: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  stock?: {
    available?: number | null;
    reserved?: number | null;
    loaned?: number | null;
    damaged?: number | null;
    total_stock?: number | null;
    available_display?: string | null;
  } | null;
  recent_movements?: InventoryMovementDetail[];
}

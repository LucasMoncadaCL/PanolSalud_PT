export interface ImplementCreatePayload {
  name: string;
  category_id: number;
  item_type: "consumable" | "reusable" | "individual";
  location_id: number;
  description: string | null;
  min_stock: number;
  observations: string | null;
}

export interface ImplementUpdatePayload {
  name: string;
  category_id: number | null;
  location_id: number;
}

export interface ImplementSummary {
  id: number;
  name: string;
  description?: string | null;
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
  } | null;
}

export type ImplementStockFilterStatus = "all" | "with_stock" | "without_stock" | "low_stock";

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
  category: {
    id: number;
    name: string;
    active: boolean;
  } | null;
  categoryId: number | null;
  locationId: number | null;
  min_stock: number | null;
  observations: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

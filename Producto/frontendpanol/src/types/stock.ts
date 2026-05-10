export type StockMovementType =
  | "increase_available"
  | "decrease_available"
  | "reserve"
  | "release_reserve"
  | "loan"
  | "return"
  | "damage"
  | "repair";

export interface StockCounters {
  total_stock: number;
  min_stock: number;
  available: number;
  reserved: number;
  loaned: number;
  damaged: number;
}

export interface IndividualItem {
  uuid: string;
  asset_code: string;
  status: "available" | "loaned" | "maintenance" | "damaged" | "blocked" | "retired";
  condition: "good" | "damaged_repairable" | "damaged_no_diagnosis" | "irreparable";
  current_location_uuid: string | null;
  active: boolean;
}

export interface StockDetail {
  implement_uuid: string;
  item_type: "consumable" | "reusable" | "individual" | null;
  stock: StockCounters;
  individuals: IndividualItem[];
}

export interface StockEntryPayload {
  quantity: number;
  asset_codes?: string[];
}

export interface StockMovementPayload {
  movement_type: StockMovementType;
  quantity?: number;
  individual_uuids?: string[];
  condition?: IndividualItem["condition"];
}

export interface IndividualUpdatePayload {
  status?: IndividualItem["status"];
  condition?: IndividualItem["condition"];
  current_location_uuid?: string | null;
  active?: boolean;
}

export interface Category {
  id: number;
  name: string;
  description?: string;
  icon?: string;
  colorHex?: string;
  imageUrl?: string;
  active: boolean;
  basePrice?: number;
  estimatedMinutes?: number;
}

export interface CategoryRequest {
  name: string;
  description?: string;
  icon?: string;
  colorHex?: string;
  imageUrl?: string;
  basePrice?: number;
  estimatedMinutes?: number;
}
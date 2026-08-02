export interface Metrics {
  tasksByStatus: Record<string, number>;
  totalRevenue: number;
  upcomingDueCount: number;
  averageRating: number | null;
}
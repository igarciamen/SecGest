export interface MessageItem {
  id: number;
  senderId: number;
  senderRole: 'ROLE_USER' | 'ROLE_ADMIN';
  content: string;
  createdAt: string;
  readAt?: string;
}

export interface ThreadDto {
  taskId: number;
  messages: MessageItem[];
}

export interface InternalNoteItem {
  id: number;
  authorUserId: number;
  content: string;
  createdAt: string;
}

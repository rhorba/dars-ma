export interface Message {
  id: string;
  senderId: string;
  body: string;
  createdAt: string;
}

export interface MessageCreateRequest {
  body: string;
}

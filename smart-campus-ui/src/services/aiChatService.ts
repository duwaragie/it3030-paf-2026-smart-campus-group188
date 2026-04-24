import { api } from '@/lib/axios';

export type ChatRole = 'user' | 'assistant';

export interface ChatMessage {
  role: ChatRole;
  content: string;
}

export interface ChatResponse {
  reply: string;
  toolsUsed: string[];
}

export const aiChatService = {
  async chat(messages: ChatMessage[]): Promise<ChatResponse> {
    const { data } = await api.post<ChatResponse>('/ai/chat', { messages });
    return data;
  },
};

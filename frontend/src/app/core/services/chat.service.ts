import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_URL } from '../config/api.config';
import { ChatState } from '../models/chat.model';
import { StorageService } from './storage.service';

const CHAT_TOKEN = 'haqua.chatToken';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  private readonly storage = inject(StorageService);

  open(): Observable<ChatState> {
    const sessionToken = this.storage.get(CHAT_TOKEN);
    return this.http
      .get<ChatState>(`${API_URL}/chat`, { params: sessionToken ? { sessionToken } : {} })
      .pipe(tap((s) => this.storage.set(CHAT_TOKEN, s.sessionToken)));
  }

  send(content: string): Observable<ChatState> {
    return this.http
      .post<ChatState>(`${API_URL}/chat/messages`, { sessionToken: this.storage.get(CHAT_TOKEN), content })
      .pipe(tap((s) => this.storage.set(CHAT_TOKEN, s.sessionToken)));
  }

  poll(after: number): Observable<ChatState> {
    return this.http.get<ChatState>(`${API_URL}/chat/${this.storage.get(CHAT_TOKEN)}/messages`, { params: { after } });
  }

  requestStaff(): Observable<void> {
    return this.http.post<void>(`${API_URL}/chat/${this.storage.get(CHAT_TOKEN)}/staff`, {});
  }

  reset(): void {
    this.storage.remove(CHAT_TOKEN);
  }
}

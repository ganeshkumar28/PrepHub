import { createClient } from './supabase/client';
import type {
  CompanyDto,
  Difficulty,
  ExperienceDto,
  ExperienceInputDto,
  ExperiencePageDto,
  ExtractionJobDto,
  Level,
  Outcome,
  ProblemDto,
  QuestionDto,
  QuestionPageDto,
  TopicDto,
  TopicKind,
  UserDto,
} from './types';

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v1';

async function getAuthHeader(): Promise<Record<string, string>> {
  if (typeof window === 'undefined') {
    return {};
  }
  try {
    const supabase = createClient();
    const {
      data: { session },
    } = await supabase.auth.getSession();
    if (session?.access_token) {
      return { Authorization: `Bearer ${session.access_token}` };
    }
  } catch (err) {
    console.error('Error fetching auth token:', err);
  }
  return {};
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  authRequired = false
): Promise<T> {
  const authHeaders = authRequired ? await getAuthHeader() : {};
  const url = `${API_BASE_URL}${path}`;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...authHeaders,
    ...((options.headers as Record<string, string>) || {}),
  };

  const response = await fetch(url, {
    ...options,
    headers,
  });

  if (!response.ok) {
    let errorDetail = `Request failed with status ${response.status}`;
    try {
      const errorJson: ProblemDto = await response.json();
      errorDetail = errorJson.detail || errorJson.title || errorDetail;
    } catch {
      // Non-JSON error body
    }
    throw new Error(errorDetail);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}

export const api = {
  // Public Experiences
  async listExperiences(params: {
    page?: number;
    size?: number;
    q?: string;
    company?: string;
    topic?: string[];
    level?: Level;
    outcome?: Outcome;
    year?: number;
    sort?: 'newest' | 'oldest';
  } = {}): Promise<ExperiencePageDto> {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.set('page', params.page.toString());
    if (params.size !== undefined) query.set('size', params.size.toString());
    if (params.q) query.set('q', params.q);
    if (params.company) query.set('company', params.company);
    if (params.topic && params.topic.length > 0) {
      params.topic.forEach((t) => query.append('topic', t));
    }
    if (params.level) query.set('level', params.level);
    if (params.outcome) query.set('outcome', params.outcome);
    if (params.year) query.set('year', params.year.toString());
    if (params.sort) query.set('sort', params.sort);

    const queryStr = query.toString();
    return request<ExperiencePageDto>(
      `/experiences${queryStr ? `?${queryStr}` : ''}`,
      {},
      false // public endpoint
    );
  },

  async getExperience(id: string): Promise<ExperienceDto> {
    return request<ExperienceDto>(`/experiences/${id}`, {}, false); // public
  },

  // Authenticated Author Actions
  async updateExperience(
    id: string,
    input: ExperienceInputDto
  ): Promise<ExperienceDto> {
    return request<ExperienceDto>(
      `/experiences/${id}`,
      {
        method: 'PUT',
        body: JSON.stringify(input),
      },
      true // requires auth
    );
  },

  async deleteExperience(id: string): Promise<void> {
    await request<void>(
      `/experiences/${id}`,
      {
        method: 'DELETE',
      },
      true // requires auth
    );
  },

  // Public Questions
  async listQuestions(params: {
    page?: number;
    size?: number;
    q?: string;
    company?: string;
    topic?: string[];
    difficulty?: Difficulty;
    sort?: 'newest';
  } = {}): Promise<QuestionPageDto> {
    const query = new URLSearchParams();
    if (params.page !== undefined) query.set('page', params.page.toString());
    if (params.size !== undefined) query.set('size', params.size.toString());
    if (params.q) query.set('q', params.q);
    if (params.company) query.set('company', params.company);
    if (params.topic && params.topic.length > 0) {
      params.topic.forEach((t) => query.append('topic', t));
    }
    if (params.difficulty) query.set('difficulty', params.difficulty);
    if (params.sort) query.set('sort', params.sort);

    const queryStr = query.toString();
    return request<QuestionPageDto>(
      `/questions${queryStr ? `?${queryStr}` : ''}`,
      {},
      false // public endpoint
    );
  },

  async getQuestion(id: string): Promise<QuestionDto> {
    return request<QuestionDto>(`/questions/${id}`, {}, false); // public
  },

  // Public Reference / Taxonomies
  async listTopics(kind?: TopicKind): Promise<TopicDto[]> {
    const query = kind ? `?kind=${encodeURIComponent(kind)}` : '';
    return request<TopicDto[]>(`/topics${query}`, {}, false); // public
  },

  async searchCompanies(q: string): Promise<CompanyDto[]> {
    return request<CompanyDto[]>(
      `/companies?q=${encodeURIComponent(q)}`,
      {},
      false // public
    );
  },

  // Authenticated Extractions
  async createExtraction(rawText: string): Promise<ExtractionJobDto> {
    return request<ExtractionJobDto>(
      '/extractions',
      {
        method: 'POST',
        body: JSON.stringify({ rawText }),
      },
      true // requires auth
    );
  },

  async getExtraction(jobId: string): Promise<ExtractionJobDto> {
    return request<ExtractionJobDto>(`/extractions/${jobId}`, {}, true); // requires auth
  },

  async publishExtraction(
    jobId: string,
    input: ExperienceInputDto
  ): Promise<ExperienceDto> {
    return request<ExperienceDto>(
      `/extractions/${jobId}/publish`,
      {
        method: 'POST',
        body: JSON.stringify(input),
      },
      true // requires auth
    );
  },

  // Authenticated User & Profile
  async getCurrentUser(): Promise<UserDto> {
    return request<UserDto>('/me', {}, true); // requires auth
  },

  async listMyExperiences(page = 0, size = 20): Promise<ExperiencePageDto> {
    return request<ExperiencePageDto>(
      `/me/experiences?page=${page}&size=${size}`,
      {},
      true // requires auth
    );
  },
};

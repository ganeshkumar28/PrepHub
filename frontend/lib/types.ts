export type Level = 'INTERN' | 'JUNIOR' | 'MID' | 'SENIOR' | 'LEAD' | 'PRINCIPAL';

export type Outcome = 'SELECTED' | 'REJECTED' | 'PENDING' | 'UNKNOWN';

export type InterviewMode = 'ONSITE' | 'REMOTE' | 'HYBRID';

export type RoundType =
  | 'ONLINE_ASSESSMENT'
  | 'TECHNICAL'
  | 'MACHINE_CODING'
  | 'SYSTEM_DESIGN'
  | 'MANAGERIAL'
  | 'HR'
  | 'BEHAVIORAL'
  | 'OTHER';

export type QuestionType =
  | 'THEORY'
  | 'CODING'
  | 'SYSTEM_DESIGN'
  | 'BEHAVIORAL'
  | 'SCENARIO'
  | 'OTHER';

export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';

export type JobStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED';

export type TopicKind =
  | 'LANGUAGE'
  | 'FRAMEWORK'
  | 'CONCEPT'
  | 'DATABASE'
  | 'SYSTEM_DESIGN'
  | 'DSA'
  | 'DEVOPS'
  | 'BEHAVIORAL'
  | 'OTHER';

export interface UserDto {
  id: string;
  displayName: string;
}

export interface CompanyDto {
  slug: string;
  name: string;
}

export interface TopicDto {
  slug: string;
  name: string;
  kind: TopicKind;
}

export interface PageMetaDto {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface QuestionDto {
  id: string;
  experienceId: string;
  text: string;
  questionType: QuestionType;
  difficulty?: Difficulty | null;
  topics?: TopicDto[];
  company?: CompanyDto;
}

export interface QuestionPageDto {
  content: QuestionDto[];
  page: PageMetaDto;
}

export interface RoundDto {
  roundNumber: number;
  roundType: RoundType;
  durationMinutes?: number | null;
  notes?: string | null;
  questions: QuestionDto[];
}

export interface ExperienceDto {
  id: string;
  author?: UserDto | null;
  isAnonymous?: boolean | null;
  company: CompanyDto;
  roleTitle: string;
  level?: Level | null;
  yearsOfExperience?: number | null;
  location?: string | null;
  interviewYear?: number | null;
  interviewMonth?: number | null;
  interviewMode?: InterviewMode | null;
  outcome: Outcome;
  summary?: string | null;
  rounds: RoundDto[];
  createdAt: string;
}

export interface ExperiencePageDto {
  content: ExperienceDto[];
  page: PageMetaDto;
}

export interface QuestionInputDto {
  text: string;
  questionType: QuestionType;
  difficulty?: Difficulty | null;
  topicSlugs: string[];
}

export interface RoundInputDto {
  roundNumber: number;
  roundType: RoundType;
  durationMinutes?: number | null;
  notes?: string | null;
  questions: QuestionInputDto[];
}

export interface ExperienceInputDto {
  companyName: string;
  roleTitle?: string | null;
  level?: Level | null;
  yearsOfExperience?: number | null;
  location?: string | null;
  interviewYear?: number | null;
  interviewMonth?: number | null;
  interviewMode?: InterviewMode | null;
  outcome: Outcome;
  summary?: string | null;
  isAnonymous?: boolean | null;
  rounds: RoundInputDto[];
}

export interface ExtractionResultDto {
  isInterviewContent: boolean;
  confidence?: number | null;
  experience?: ExperienceInputDto | null;
  suggestedNewTopics?: string[];
  warnings?: string[];
}

export interface ExtractionJobDto {
  id: string;
  status: JobStatus;
  result?: ExtractionResultDto | null;
  errorCode?: string | null;
  createdAt: string;
  completedAt?: string | null;
}

export interface ProblemDto {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
}


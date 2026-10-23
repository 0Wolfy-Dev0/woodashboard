import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import {
  Check,
  CheckCheck,
  Circle,
  ListFilter,
  LoaderCircle,
  Plus,
  Search,
  Sparkles,
  SquarePen,
} from 'lucide-react'
import { api } from '../api'
import { formatDate } from '../format'
import { useAutoRefresh } from '../hooks/useAutoRefresh'
import type { Group, TodoItem, ToastNotice } from '../types'

type TodoFilter = 'all' | 'open' | 'completed'

interface TodosPageProps {
  groups: Group[]
  refreshVersion: number
  onCreateTodo: () => void
  onEditTodo: (todo: TodoItem) => void
  onNotice: (message: string, kind?: ToastNotice['kind']) => void
}

export function TodosPage({ groups, refreshVersion, onCreateTodo, onEditTodo, onNotice }: TodosPageProps) {
  const [filter, setFilter] = useState<TodoFilter>('open')
  const [groupId, setGroupId] = useState('')
  const [todos, setTodos] = useState<TodoItem[]>([])
  const [search, setSearch] = useState('')
  const [loading, setLoading] = useState(true)
  const loadedRef = useRef(false)

  const loadTodos = useCallback(async (background = false) => {
    if (!loadedRef.current) setLoading(true)
    const completed = filter === 'all' ? undefined : filter === 'completed'
    try {
      const result = await api.listAccessibleTodos(completed, groupId || undefined)
      setTodos(result)
      loadedRef.current = true
    } catch (error) {
      if (!background) onNotice(error instanceof Error ? error.message : 'Could not load tasks.', 'error')
    } finally {
      setLoading(false)
    }
  }, [filter, groupId, onNotice])

  useEffect(() => {
    void loadTodos()
  }, [loadTodos, refreshVersion])

  useEffect(() => {
    if (groupId && !groups.some((group) => group.id === groupId)) setGroupId('')
  }, [groupId, groups])

  useAutoRefresh(() => loadTodos(true))

  const filteredTodos = useMemo(() => {
    const query = search.trim().toLocaleLowerCase()
    if (!query) return todos
    return todos.filter((todo) => `${todo.name} ${todo.description ?? ''}`.toLocaleLowerCase().includes(query))
  }, [todos, search])

  async function toggle(todo: TodoItem) {
    try {
      const updated = await api.updateTodo(todo.id, { completed: !todo.completed })
      if (filter !== 'all' && updated.completed !== (filter === 'completed')) {
        setTodos((items) => items.filter((item) => item.id !== todo.id))
      } else {
        setTodos((items) => items.map((item) => item.id === updated.id ? updated : item))
      }
      onNotice(updated.completed ? 'Task completed. Nice work.' : 'Task moved back to your list.')
    } catch (error) {
      onNotice(error instanceof Error ? error.message : 'Could not update this task.', 'error')
    }
  }

  const countLabel = filter === 'open' ? 'to do' : filter === 'completed' ? 'completed' : 'tasks'

  return (
    <div className="page-content todos-page">
      <div className="page-heading">
        <div>
          <span className="eyebrow">ONE THING AT A TIME</span>
          <h1>Your to-do list</h1>
          <p>Keep the small things moving, without carrying them all in your head.</p>
        </div>
        <button className="button button-primary" onClick={onCreateTodo}><Plus size={17} /> Add a task</button>
      </div>

      <section className="surface-card todo-board">
        <div className="todo-toolbar">
          <div className="filter-tabs" role="tablist" aria-label="Filter tasks">
            {([
              ['all', 'All tasks'], ['open', 'To do'], ['completed', 'Completed'],
            ] as const).map(([key, label]) => (
              <button className={`filter-tab${filter === key ? ' active' : ''}`} key={key} role="tab" aria-selected={filter === key} onClick={() => setFilter(key)}>
                {key === 'completed' ? <CheckCheck size={15} /> : key === 'open' ? <Circle size={14} /> : <ListFilter size={15} />}
                {label}
              </button>
            ))}
          </div>
          <div className="todo-toolbar-right">
            <select className="select-control" value={groupId} onChange={(e) => setGroupId(e.target.value)} aria-label="Filter tasks by group">
              <option value="">All lists</option>
              {groups.map((group) => <option key={group.id} value={group.id}>{group.name}</option>)}
            </select>
            <label className="search-field todo-search">
              <Search size={16} />
              <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Find a task" aria-label="Search tasks" />
            </label>
          </div>
        </div>

        <div className="todo-list-heading"><span>{loading ? 'Loading tasks…' : `${filteredTodos.length} ${countLabel}`}</span><span>FILTERED BY COMPLETION ON THE SERVER</span></div>

        {loading ? (
          <div className="todo-loading"><LoaderCircle className="spin" size={20} /> Loading your tasks…</div>
        ) : filteredTodos.length === 0 ? (
          <div className="empty-state todo-empty">
            <span className="empty-icon">{filter === 'completed' ? <CheckCheck size={21} /> : <Sparkles size={21} />}</span>
            <strong>{search ? 'No tasks match that search' : filter === 'completed' ? 'No completed tasks yet' : 'Your list is clear'}</strong>
            <p>{search ? 'Try another word or clear the search.' : filter === 'completed' ? 'Completed tasks will find their way here.' : 'Add a task when something comes to mind.'}</p>
            {!search && filter !== 'completed' && <button className="button button-secondary button-small" onClick={onCreateTodo}><Plus size={15} /> Add a task</button>}
          </div>
        ) : (
          <div className="todo-list">
            {filteredTodos.map((todo) => (
              <article className={`todo-row${todo.completed ? ' todo-row-complete' : ''}`} key={todo.id}>
                <button className={`todo-toggle${todo.completed ? ' checked' : ''}`} onClick={() => void toggle(todo)} aria-label={todo.completed ? `Reopen ${todo.name}` : `Complete ${todo.name}`}>
                  {todo.completed ? <Check size={15} /> : <Circle size={20} />}
                </button>
                <button className="todo-row-content" onClick={() => onEditTodo(todo)}>
                  <strong>{todo.name}</strong>
                  {todo.description && <span>{todo.description}</span>}
                </button>
                <span className="todo-created">Added {formatDate(todo.creationTime)}</span>
                <button className="icon-button todo-edit" onClick={() => onEditTodo(todo)} aria-label={`Edit ${todo.name}`}><SquarePen size={17} /></button>
              </article>
            ))}
          </div>
        )}
      </section>

      <div className="todo-encouragement"><span className="encouragement-icon"><Sparkles size={16} /></span><span>Progress is progress, no matter how small.</span></div>
    </div>
  )
}

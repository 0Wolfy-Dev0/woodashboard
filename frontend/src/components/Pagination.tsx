import type { PageResult } from '../types'

interface PaginationProps<T> {
  data: PageResult<T> | null
  onPageChange: (page: number) => void
  label?: string
}

export function Pagination<T>({ data, onPageChange, label }: PaginationProps<T>) {
  if (!data || data.total === 0) return null
  return (
    <div className="pagination-bar">
      <button className="button button-secondary button-small" type="button" disabled={data.page <= 0} onClick={() => onPageChange(data.page - 1)}>Previous</button>
      <span>{label ? `${label} · ` : ''}Page {data.page + 1} of {data.totalPages} · {data.total} total</span>
      <button className="button button-secondary button-small" type="button" disabled={data.page + 1 >= data.totalPages} onClick={() => onPageChange(data.page + 1)}>Next</button>
    </div>
  )
}

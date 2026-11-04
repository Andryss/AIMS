import { formatUserLabel as userLoginFromMap } from '../../utils/users';
import React from 'react';
import { useIncidentComments } from '../../hooks/useIncidentComments';
import { formatDateTime as formatDate } from '../../utils/dateTime';

import { UserChip } from '../UserChip';
import { LoadingBlock } from '../ui/LoadingBlock';
import { EmptyState } from '../ui/EmptyState';
import { FormField } from '../ui/FormField';
import { Button } from '../ui/Button';

type Props = ReturnType<typeof useIncidentComments> & { canComment: boolean; usersMap: Map<number, string> };
export function IncidentComments({ comments, commentsLoading, commentText, setCommentText, commentSubmitting,
  handleAddComment, canComment, usersMap }: Props) {
  return <>
    {commentsLoading && <LoadingBlock label="Загрузка комментариев…" inline />}
    {!commentsLoading && comments.length === 0 && (
      <EmptyState title="Комментариев пока нет" />
    )}
    <ul className="comment-list">
      {comments.map((comment) => (
        <li key={comment.id} className="comment-list__item">
          <div className="comment-list__meta">
            <UserChip login={userLoginFromMap(comment.authorUserId, usersMap)} size={24} />
            <span className="text-muted">{formatDate(comment.createdAt)}</span>
          </div>
          <p className="comment-list__text">{comment.text}</p>
        </li>
      ))}
    </ul>

    {canComment && (
      <form className="comment-form" onSubmit={handleAddComment}>
        <FormField label="Добавить комментарий">
          <textarea
            value={commentText}
            onChange={(e) => setCommentText(e.target.value)}
            rows={3}
            disabled={commentSubmitting}
          />
        </FormField>
        <Button
          type="submit"
          variant="primary"
          loading={commentSubmitting}
          disabled={!commentText.trim()}
        >
          Отправить
        </Button>
      </form>
    )}
  </>;
}

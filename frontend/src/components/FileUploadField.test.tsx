import React, { useState } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { FileUploadField } from './FileUploadField';

function Wrapper({ disabled = false }: { disabled?: boolean }) {
  const [files, setFiles] = useState<File[]>([]);
  return (
    <FileUploadField label="Вложения" files={files} onChange={setFiles} disabled={disabled} />
  );
}

describe('FileUploadField', () => {
  it('adds and removes files', async () => {
    render(<Wrapper />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File(['data'], 'photo.jpg', { type: 'image/jpeg' });

    await userEvent.upload(input, file);
    expect(screen.getByText('photo.jpg')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /Удалить photo.jpg/i }));
    expect(screen.queryByText('photo.jpg')).not.toBeInTheDocument();
  });

  it('adds files via drag and drop', () => {
    render(<Wrapper />);
    const dropzone = screen.getByRole('button', { name: /Перетащите файлы/i });
    const file = new File(['data'], 'drop.txt', { type: 'text/plain' });

    fireEvent.dragOver(dropzone);
    fireEvent.drop(dropzone, {
      dataTransfer: {
        files: [file],
        clearData: jest.fn(),
      },
    });

    expect(screen.getByText('drop.txt')).toBeInTheDocument();
  });

  it('shows kilobyte size and ignores duplicate files', async () => {
    render(<Wrapper />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    const file = new File([new ArrayBuffer(2048)], 'large.bin', { type: 'application/octet-stream' });

    await userEvent.upload(input, file);
    expect(screen.getByText(/КБ/)).toBeInTheDocument();

    await userEvent.upload(input, file);
    expect(screen.getAllByText('large.bin')).toHaveLength(1);
  });

  it('ignores drop when disabled', () => {
    render(<Wrapper disabled />);
    const dropzone = screen.getByRole('button', { name: /Перетащите файлы/i });
    const file = new File([new ArrayBuffer(1024)], 'blocked.bin');

    fireEvent.drop(dropzone, {
      dataTransfer: {
        files: [file],
        clearData: jest.fn(),
      },
    });
    expect(screen.queryByText('blocked.bin')).not.toBeInTheDocument();
  });

  it('shows megabyte size label', () => {
    render(<Wrapper />);
    const dropzone = screen.getByRole('button', { name: /Перетащите файлы/i });
    const file = new File([new ArrayBuffer(2 * 1024 * 1024)], 'huge.bin');

    fireEvent.drop(dropzone, {
      dataTransfer: {
        files: [file],
        clearData: jest.fn(),
      },
    });
    expect(screen.getByText(/МБ/)).toBeInTheDocument();
  });

  it('shows byte size label for tiny files', async () => {
    render(<Wrapper />);
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    await userEvent.upload(input, new File([new ArrayBuffer(10)], 'tiny.bin'));
    expect(screen.getByText(/10 Б/)).toBeInTheDocument();
  });

  it('toggles drag-active class and opens picker from keyboard', async () => {
    render(<Wrapper />);
    const dropzone = screen.getByRole('button', { name: /Перетащите файлы/i });

    fireEvent.dragOver(dropzone);
    expect(dropzone).toHaveClass('file-upload__dropzone--active');

    fireEvent.dragLeave(dropzone);
    expect(dropzone).not.toHaveClass('file-upload__dropzone--active');

    const clickSpy = jest.spyOn(HTMLInputElement.prototype, 'click');
    dropzone.focus();
    await userEvent.keyboard('{Enter}');
    expect(clickSpy).toHaveBeenCalled();
    clickSpy.mockRestore();
  });

  it('ignores empty drop payload', () => {
    render(<Wrapper />);
    const dropzone = screen.getByRole('button', { name: /Перетащите файлы/i });
    fireEvent.drop(dropzone, {
      dataTransfer: {
        files: [],
        clearData: jest.fn(),
      },
    });
    expect(screen.queryByRole('listitem')).not.toBeInTheDocument();
  });
});

import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import MediaGallery, { type MediaItem } from './MediaGallery';

describe('MediaGallery', () => {
  it('renders video preview and updates title', () => {
    const onChange = vi.fn();
    const value: MediaItem[] = [
      {
        id: 'media-1',
        url: 'https://example.com/demo.mp4',
        type: 'video',
        title: { 'zh-CN': '演示视频' }
      }
    ];

    render(<MediaGallery value={value} onChange={onChange} locales={['zh-CN', 'en-US']} />);

    expect(screen.getByTestId('media-preview-video-media-1')).toBeInTheDocument();

    const titleInput = screen.getByTestId('l10n-input-field');
    fireEvent.change(titleInput, { target: { value: '新视频标题' } });

    expect(onChange).toHaveBeenCalledWith([
      {
        id: 'media-1',
        url: 'https://example.com/demo.mp4',
        type: 'video',
        title: { 'zh-CN': '新视频标题' }
      }
    ]);
  });

  it('adds a media item', () => {
    const onChange = vi.fn();
    render(<MediaGallery value={[]} onChange={onChange} />);

    fireEvent.click(screen.getByRole('button', { name: /add media/i }));

    expect(onChange).toHaveBeenCalledTimes(1);
    const payload = onChange.mock.calls[0][0] as MediaItem[];
    expect(payload).toHaveLength(1);
    expect(payload[0].type).toBe('image');
  });
});

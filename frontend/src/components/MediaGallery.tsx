import React from 'react';
import { Button, Card, Empty, Input, Radio, Space } from 'antd';
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons';
import L10nInput, { type L10nValues } from './L10nInput';

export type MediaType = 'image' | 'video';

export interface MediaItem {
  id: string;
  url: string;
  type: MediaType;
  title: L10nValues;
}

type TranslateSuggestionFn = (sourceText: string, sourceLocale: string, targetLocale: string) => Promise<string>;

export interface MediaGalleryProps {
  value?: MediaItem[];
  onChange?: (next: MediaItem[]) => void;
  locales?: string[];
  requestTranslation?: TranslateSuggestionFn;
  emptyDescriptionText?: string;
  mediaTitlePrefixText?: string;
  addMediaText?: string;
  urlPlaceholderText?: string;
  imageLabelText?: string;
  videoLabelText?: string;
  titlePlaceholderText?: string;
}

let mediaIdCounter = 0;

const createMediaId = (): string => {
  mediaIdCounter += 1;
  return typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
    ? `media-${crypto.randomUUID()}`
    : `media-${Date.now()}-${mediaIdCounter}-${Math.random().toString(36).slice(2, 8)}`;
};

const createMediaItem = (): MediaItem => ({
  id: createMediaId(),
  url: '',
  type: 'image',
  title: {}
});

export default function MediaGallery({
  value = [],
  onChange,
  locales = ['zh-CN', 'en-US'],
  requestTranslation,
  emptyDescriptionText = 'No media yet, click below to add.',
  mediaTitlePrefixText = 'Media',
  addMediaText = 'Add Media',
  urlPlaceholderText = 'https://example.com/media.jpg',
  imageLabelText = 'Image',
  videoLabelText = 'Video',
  titlePlaceholderText = 'Input media title'
}: MediaGalleryProps) {
  const updateItem = (id: string, patch: Partial<MediaItem>) => {
    const next = value.map((item) => (item.id === id ? { ...item, ...patch } : item));
    onChange?.(next);
  };

  const addItem = () => {
    onChange?.([...value, createMediaItem()]);
  };

  const removeItem = (id: string) => {
    onChange?.(value.filter((item) => item.id !== id));
  };

  return (
    <Space direction="vertical" style={{ width: '100%' }} size={12}>
      {value.length === 0 ? (
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description={emptyDescriptionText} />
      ) : (
        value.map((item) => (
          <Card
            key={item.id}
            size="small"
            title={`${mediaTitlePrefixText} #${item.id.slice(-4)}`}
            extra={
              <Button
                type="text"
                danger
                icon={<DeleteOutlined />}
                onClick={() => removeItem(item.id)}
                aria-label={`delete-media-${item.id}`}
              />
            }
          >
            <Space direction="vertical" style={{ width: '100%' }} size={8}>
              <Input
                value={item.url}
                placeholder={urlPlaceholderText}
                onChange={(event) => updateItem(item.id, { url: event.target.value })}
              />
              <Radio.Group
                value={item.type}
                onChange={(event) => updateItem(item.id, { type: event.target.value as MediaType })}
                optionType="button"
                buttonStyle="solid"
              >
                <Radio.Button value="image">{imageLabelText}</Radio.Button>
                <Radio.Button value="video">{videoLabelText}</Radio.Button>
              </Radio.Group>
              {item.url ? (
                item.type === 'video' ? (
                  <video
                    data-testid={`media-preview-video-${item.id}`}
                    src={item.url}
                    controls
                    style={{ width: '100%', maxHeight: 180, borderRadius: 8 }}
                  />
                ) : (
                  <img
                    data-testid={`media-preview-image-${item.id}`}
                    src={item.url}
                    alt="media-preview"
                    style={{ width: '100%', maxHeight: 180, objectFit: 'cover', borderRadius: 8 }}
                  />
                )
              ) : null}
              <L10nInput
                value={item.title}
                onChange={(title) => updateItem(item.id, { title })}
                locales={locales}
                requestTranslation={requestTranslation}
                placeholder={titlePlaceholderText}
              />
            </Space>
          </Card>
        ))
      )}
      <Button icon={<PlusOutlined />} onClick={addItem}>
        {addMediaText}
      </Button>
    </Space>
  );
}

from paddleocr import PaddleOCR
import sys
import os

# Initialize PaddleOCR with Japanese support
# lang='japan' for Japanese, 'en' for English
ocr = PaddleOCR(lang='japan', device='cpu', enable_mkldnn=False)

img_path = 'C:\\workspace\\bobbuy\\sample\\IMG_1487.jpg'

print(f"Processing {img_path}...")
result = ocr.ocr(img_path)

output = []
if result:
    for res in result:
        texts = res.get('rec_texts', [])
        scores = res.get('rec_scores', [])
        for text, score in zip(texts, scores):
            output.append(f"Text: {text} (Conf: {score:.2f})")

with open('ocr_result.txt', 'w', encoding='utf-8') as f:
    f.write('\n'.join(output))
print("Results saved to ocr_result.txt")

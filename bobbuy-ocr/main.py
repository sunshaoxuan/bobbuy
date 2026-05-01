from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from paddleocr import PaddleOCR
import base64
import io
from PIL import Image
import numpy as np
import os
from threading import Lock

app = FastAPI(title="BOBBuy OCR Service")

ocr = None
ocr_init_error = None
ocr_lock = Lock()


def get_ocr():
    global ocr, ocr_init_error
    if ocr is not None:
        return ocr
    with ocr_lock:
        if ocr is not None:
            return ocr
        try:
            ocr = PaddleOCR(
                lang='japan',
                device='cpu',
                enable_mkldnn=False,
                use_angle_cls=False,
                ocr_version='PP-OCRv3'
            )
            ocr_init_error = None
            return ocr
        except Exception as exc:
            ocr_init_error = str(exc)
            raise

class OCRRequest(BaseModel):
    image: str  # base64 encoded image

@app.post("/ocr")
async def perform_ocr(request: OCRRequest):
    try:
        # Decode base64 image
        try:
            image_data = base64.b64decode(request.image)
        except Exception as e:
            raise HTTPException(status_code=400, detail=f"Invalid base64: {str(e)}")

        image = Image.open(io.BytesIO(image_data)).convert('RGB')
        img_np = np.array(image)
        
        # Perform OCR
        try:
            result = get_ocr().ocr(img_np)
        except Exception as exc:
            raise HTTPException(status_code=503, detail=f"OCR model unavailable: {str(exc)}")
        
        # Format results
        output = []
        if result:
            if isinstance(result, list):
                for res in result:
                    if res is None: continue
                    # Handle Paddlex OCRResult object or dict-like object
                    if hasattr(res, 'get') or isinstance(res, dict) or 'paddlex' in str(type(res)).lower():
                        res_dict = res if isinstance(res, dict) else (res.json() if hasattr(res, 'json') else vars(res))
                        texts = res_dict.get('rec_texts', [])
                        scores = res_dict.get('rec_scores', [])
                        # Fallback for alternative structures
                        if not texts and 'rec_res' in res_dict:
                            texts = [item.get('text') for item in res_dict['rec_res']]
                            scores = [item.get('score') for item in res_dict['rec_res']]
                        
                        for t, s in zip(texts, scores):
                            output.append({"text": t, "confidence": float(s)})
                    elif isinstance(res, list):
                        # Legacy list-based result structure [[[box], [text, score]], ...]
                        for line in res:
                            if isinstance(line, (list, tuple)) and len(line) > 1:
                                try:
                                    text = line[1][0]
                                    conf = line[1][1]
                                    output.append({"text": text, "confidence": float(conf)})
                                except: pass
            elif isinstance(result, dict):
                texts = result.get('rec_texts', [])
                scores = result.get('rec_scores', [])
                for t, s in zip(texts, scores):
                    output.append({"text": t, "confidence": float(s)})
        
        return {"results": output}
    except Exception as e:
        print(f"OCR Error: {str(e)}")
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/health")
async def health_check():
    return {"status": "ok", "ocrReady": ocr is not None, "ocrInitError": ocr_init_error}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)

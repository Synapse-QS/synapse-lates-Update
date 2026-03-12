import "jsr:@supabase/functions-js/edge-runtime.d.ts";

Deno.serve(async (req: Request) => {
  const { userId, message } = await req.json();

  return new Response(
    JSON.stringify({
      success: true,
      notification: {
        userId,
        message: message || "Test notification sent!",
        timestamp: new Date().toISOString(),
      },
    }),
    {
      headers: {
        "Content-Type": "application/json",
        "Connection": "keep-alive",
      },
    }
  );
});

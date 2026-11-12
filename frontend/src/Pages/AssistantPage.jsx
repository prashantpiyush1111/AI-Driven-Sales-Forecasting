import PageHeading from "../components/common/PageHeading";

const exampleQuestions = [
  "Which products are growing fastest?",
  "Summarize next quarter’s forecast.",
  "Compare revenue by region.",
];

export default function AssistantPage() {
  return (
    <div className="page-content">
      <PageHeading eyebrow="NATURAL LANGUAGE ANALYTICS" title="AI assistant"
        description="Ask questions about sales performance in plain language." />
      <section className="assistant-empty panel">
        <span className="assistant-spark">✧</span>
        <div className="eyebrow">INTEGRATION IN PROGRESS</div>
        <h2>Your business data, in conversation.</h2>
        <p>The assistant will connect to the RAG service after the backend team publishes the chat API. It will answer from your sales and forecast data with supporting context.</p>
        <div className="prompt-examples">
          {exampleQuestions.map((question) => <span key={question}>{question}</span>)}
        </div>
        <button className="disabled-send" disabled>Chat integration not connected</button>
      </section>
    </div>
  );
}
